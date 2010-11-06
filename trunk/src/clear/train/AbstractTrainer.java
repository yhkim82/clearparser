package clear.train;

import clear.model.AbstractModel;
import clear.train.algorithm.IAlgorithm;
import clear.train.kernel.AbstractKernel;

abstract public class AbstractTrainer
{
	volatile protected AbstractKernel k_kernel;
	volatile protected IAlgorithm     a_alg;
	volatile protected AbstractModel  m_model;
	
	public AbstractTrainer(AbstractKernel kernel, IAlgorithm alg, String modelFile, int numThreads)
	{
		k_kernel = kernel;
		a_alg    = alg;
		
		initModel(kernel);
		train(modelFile, numThreads);
	}
	
	abstract protected void initModel(AbstractKernel kernel);
	abstract protected void train(String modelFile, int numThreads);
}
