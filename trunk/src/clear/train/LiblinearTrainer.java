package clear.train;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import clear.model.OneVsAllModel;

public class LiblinearTrainer extends AbstractTrainer
{
	private final int MAX_ITER = 1000;
	
	volatile private OneVsAllModel m_model;
//	volatile private Random        r_rand;
	
	/** 1 - L1-loss, 2: L2-loss */
	private byte   loss_type;
	/** Regularization parameter */
	private double  c;
	/** Termination criterion */
	private double  eps;
	/** if true, calculate bias */
	private boolean bias;
	
	public LiblinearTrainer(String instanceFile, String modelFile, int numThreads, byte lossType, double c, double eps, boolean bias)
	{
		super(instanceFile);
		
		init(lossType, c, eps, bias);
		train(modelFile, numThreads);
	}
	
	private void init(byte lossType, double c, double eps, boolean bias)
	{
		m_model   = new OneVsAllModel(s_labels.size(), D);
	//	r_rand    = new Random(0);
	
		loss_type = lossType;
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

	private double[] getWeight(short currLabel)
	{
		double[] QD     = new double[N];
		double[] alpha  = new double[N];
		double[] weight = new double[D];
		double U, G, d;
		
		int [] index = new int [N];
		byte[] y     = new byte[N];
		
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
			y    [i] = (a_labels.get(i) == currLabel) ? (byte)1 : (byte)-1;
			QD   [i] = diag[GETI(y, i)] + a_features.get(i).length;
			index[i] = i;
		}
		
		for (iter=0; iter<MAX_ITER; iter++)
		{
			PGmax_new = Double.NEGATIVE_INFINITY;
			PGmin_new = Double.POSITIVE_INFINITY;
			
		/*	for (i=0; i<active_size; i++)
			{
				int j = i + r_rand.nextInt(active_size - i);
				swap(index, i, j);
			}*/
			
			for (s=0; s<active_size; s++)
			{
				i = index[s];
				byte yi = y[i];

				G = weight[0];	// if (bias) ? bias : 0
				for (int idx : a_features.get(i))
					G += weight[idx];
				G = G * yi - 1;
				G += alpha[i] * diag[GETI(y, i)];
				U = upper_bound[GETI(y, i)];
				
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
					
					if (bias)							weight[0  ] += d;
					for (int idx : a_features.get(i))	weight[idx] += d;
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
			v += alpha[i] * (alpha[i] * diag[GETI(y, i)] - 2);
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
	
	private int GETI(byte[] y, int i)
	{
		return y[i] + 1;
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
		
		/**
		 * Trains RRM model using {@link AbstractTrainer#a_features} and {@link AbstractTrainer#a_labels} with respect to <code>currLabel</code>.
		 * @param currLabel current label to train ({@link this#curr_label})
		 */
		public TrainTask(short currLabel)
		{
			curr_label = currLabel;
		}
		
		public void run()
		{
			m_model.copyWeight(curr_label, getWeight(curr_label));
		}
	}
}
