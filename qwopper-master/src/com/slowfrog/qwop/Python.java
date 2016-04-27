import org.python.core.PyInstance;  
import org.python.util.PythonInterpreter;  


public class Python  
{
	PythonInterpreter interpreter = null;  
	
	public Python()  
	{
		PythonInterpreter.initialize(System.getProperties(),
		System.getProperties(), new String[0]);
		this.interpreter = new PythonInterpreter();
	}

	void execfile( final String fileName )
	{
		this.interpreter.execfile(fileName);
	}

	PyInstance createClass( final String className, final String opts )  
	{  
		return (PyInstance) this.interpreter.eval(className + "(" + opts + ")");  
	}
   
	void initiate(int popSize) {
		Python ie = new Python();
		ie.execfile("ga.py");
		PyInstance ga = ie.createClass("ga", "None");
		ga.invoke("initiate", popSize);
	}
	
	void crossoverMutation(float crossoverRate, float mutationRate) {
		Python ie = new Python();
		ie.execfile("ga.py");
		PyInstance ga = ie.createClass("ga", "None");
		ga.invoke("crossoverMutate");
	}

	public static void main( String gargs[] )
	{  
	}
}