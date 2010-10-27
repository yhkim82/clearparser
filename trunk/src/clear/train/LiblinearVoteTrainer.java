package clear.train;

import gnu.trove.list.array.TByteArrayList;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import clear.model.OneVsAllVoteModel;

public class LiblinearVoteTrainer extends AbstractTrainer
{
	private final int MAX_ITER = 1000;
	
	volatile private OneVsAllVoteModel m_model;
	volatile private Random            r_rand;
	
	/** 1 - L1-loss, 2: L2-loss */
	private byte   loss_type;
	/** Number of votes */
	private int     n_votes;
	/** Regularization parameter */
	private double  c;
	/** Termination criterion */
	private double  eps;
	/** if true, calculate bias */
	private boolean bias;
	
	public LiblinearVoteTrainer(String instanceFile, String modelFile, int numThreads, byte lossType, int numVotes, double c, double eps, boolean bias)
	{
		super(instanceFile);
		
		init(lossType, numVotes, c, eps, bias);
		train(modelFile, numThreads);
	}
	
	private void init(byte lossType, int numVotes, double c, double eps, boolean bias)
	{
		m_model   = new OneVsAllVoteModel(s_labels.size(), D, numVotes);
		r_rand    = new Random(0);
	
		loss_type = lossType;
		n_votes   = numVotes;
		this.c    = c;
		this.eps  = eps;
		this.bias = bias;
	}

	private void train(String modelFile, int numThreads)
	{
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);;
		System.out.println("\n* Training");
		
		for (short currLabel : s_labels.toArray())
			executor.execute(new TrainTask(currLabel));

		executor.shutdown();
		
		try
		{
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("\n * Saving: "+modelFile);
			m_model.save(modelFile);
		}
		catch (InterruptedException e) {e.printStackTrace();}
	}

	private double[] getWeight(short currLabel, ArrayList<int[]> a_x, TByteArrayList a_y)
	{
		double[] QD     = new double[N];
		double[] alpha  = new double[N];
		double[] weight = new double[D];
		double U, G, d;
		
		int [] index = new int [N];
		
		int active_size = N;
		int i, s, iter;
		
		// PG: projected gradient, for shrinking and stopping
		double PG;
		double PGmax_old = Double.POSITIVE_INFINITY;
		double PGmin_old = Double.NEGATIVE_INFINITY;
		double PGmax_new, PGmin_new;
		
		// for loss function
		double[] diag        = {0, 0, 0};
		double[] upper_bound = {c, 0, c};
		
		if (loss_type == 2)
		{
			diag[0] = 0.5 / c;
			diag[2] = 0.5 / c;
			upper_bound[0] = Double.POSITIVE_INFINITY;
			upper_bound[2] = Double.POSITIVE_INFINITY;
		}
		
		for (i=0; i<N; i++)
		{
			QD   [i] = diag[GETI(a_y, i)] + a_x.get(i).length;
			index[i] = i;
		}
		
		for (iter=0; iter<MAX_ITER; iter++)
		{
			PGmax_new = Double.NEGATIVE_INFINITY;
			PGmin_new = Double.POSITIVE_INFINITY;
			
			for (s=0; s<active_size; s++)
			{
				i = index[s];
				byte yi = a_y.get(i);

				G = weight[0];	// if (bias) ? bias : 0
				for (int idx : a_x.get(i))
					G += weight[idx];
				G = G * yi - 1;
				G += alpha[i] * diag[GETI(a_y, i)];
				U = upper_bound[GETI(a_y, i)];
				
				if (alpha[i] == 0)
				{
					if (G > PGmax_old)
					{
						active_size--;
						swap(index, s, active_size);
						s--;
						continue;
					}
					
					PG = Math.min(G, 0);
                }
				else if (alpha[i] == U)
				{
					if (G < PGmin_old)
					{
						active_size--;
						swap(index, s, active_size);
						s--;
						continue;
					}
					
					PG = Math.max(G, 0);
				}
				else
				{
					PG = G;
				}
				
				PGmax_new = Math.max(PGmax_new, PG);
				PGmin_new = Math.min(PGmin_new, PG);
				
				if (Math.abs(PG) > 1.0e-12)
				{
					double alpha_old = alpha[i];
					alpha[i] = Math.min(Math.max(alpha[i] - G / QD[i], 0.0), U);
					d = (alpha[i] - alpha_old) * yi;
					
					if (bias)					weight[0  ] += d;
					for (int idx : a_x.get(i))	weight[idx] += d;
				}
			}
			
			if (PGmax_new - PGmin_new <= eps)
			{
				if (active_size == N)
					break;
				else
				{
					active_size = N;
					PGmax_old = Double.POSITIVE_INFINITY;
					PGmin_old = Double.NEGATIVE_INFINITY;
					continue;
				}
			}
			
			PGmax_old = PGmax_new;
			PGmin_old = PGmin_new;
			if (PGmax_old <= 0) PGmax_old = Double.POSITIVE_INFINITY;
			if (PGmin_old >= 0) PGmin_old = Double.NEGATIVE_INFINITY;
		}
		
		double v = 0;
		int  nSV = 0;
		
		for (double w : weight)	v += w * w;
		for (i = 0; i < N; i++)
		{
			v += alpha[i] * (alpha[i] * diag[GETI(a_y, i)] - 2);
			if (alpha[i] > 0) ++nSV;
		}
		
		StringBuilder build = new StringBuilder();
		
		build.append("- label = ");
		build.append(currLabel);
		build.append(": iter = ");
		build.append(iter);
		build.append(", nSV = ");
		build.append(nSV);

		System.out.println(build.toString());
		
		return weight;
	}
	
	private int GETI(TByteArrayList y, int i)
	{
		return y.get(i) + 1;
	}
	
	private void swap(ArrayList<int[]> aX, TByteArrayList aY, int idxA, int idxB)
	{
		int[] t = aX.get(idxA);
		aX.set(idxA, aX.get(idxB));
		aX.set(idxB, t);
		
		byte s = aY.get(idxA);
		aY.set(idxA, aY.get(idxB));
		aY.set(idxB, s);
	}
	
	private void swap(int[] array, int idxA, int idxB)
	{
		int temp    = array[idxA];
		array[idxA] = array[idxB];
		array[idxB] = temp;
	}
		
	class TrainTask implements Runnable
	{
		/** Current label to train */
		short curr_label;
		
		ArrayList<int[]> a_xp;
		ArrayList<int[]> a_xn;
		
		/**
		 * Trains RRM model using {@link AbstractTrainer#a_features} and {@link AbstractTrainer#a_labels} with respect to <code>currLabel</code>.
		 * @param currLabel current label to train ({@link this#curr_label})
		 */
		public TrainTask(short currLabel)
		{
			curr_label = currLabel;
			a_xp = new ArrayList<int[]>();
			a_xn = new ArrayList<int[]>();
			int i;
			
			for (i=0; i<N; i++)
			{
				int[] x = a_features.get(i);
				short y = a_labels  .get(i);
				
				if (y == curr_label)	a_xp.add(x);
				else					a_xn.add(x);
			}
		}
		
		public void run()
		{
			for (int vote = 0; vote < n_votes; vote++)
			{
				ArrayList<int[]> aX = new ArrayList<int[]>(N);
				TByteArrayList   aY = new TByteArrayList  (N);
				int pSize = a_xp.size(), nSize = a_xn.size(), i, j;
				
				for (i=0; i<pSize; i++)
				{
					aX.add(a_xp.get(r_rand.nextInt(pSize)));
					aY.add((byte)1);
				}
				
				for (i=0; i<nSize; i++)
				{
					aX.add(a_xn.get(r_rand.nextInt(nSize)));
					aY.add((byte)-1);
				}
				
				for (i=0; i<N; i++)
				{
					j = i + r_rand.nextInt(N - i);
					swap(aX, aY, i, j);
				}
				
				m_model.copyWeight(curr_label, vote, getWeight(curr_label, aX, aY));
			}
		}
	}
}
