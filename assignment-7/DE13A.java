// DE13A.java CS5125/6025 Cheng 2025
// GF(2^8) for RS for QR
// computes error correction code for user message 
// this version can only correct error in one byte 
// Usage: java DE13A message

import java.io.*;
import java.util.*;

public class DE13A{
	GF f = new GF(256, 2, 0x11d);
	static int Gdegree = 2;  // generator polynomial degree, indicating maximum 8 errors to correct
	int numberOfErrors = 1; // number of random errors
	HashMap<Integer, Integer> errors = new HashMap<Integer, Integer>(); // random errors
	Polynomial G = makeRSG(); // the generator
	Polynomial M = null; // message
	Polynomial C = null; // reed-solomon encoded
	Polynomial CplusE = null; // error added
	Polynomial Syndrome = null; // syndrome after error added

class GF{  // finite field of 2^k elements so that addition and subtractions are XOR
	int fieldSize = 0;  // 2^k
	public int logBase = 0;  // a primative element
	int irreducible = 0;  // a irreducible polynomial of degree k
	public int[] alog = null;  // all powers of the logBase
	public int[] log = null;  // discrete log, log[0] is not defined

	public GF(int size, int base, int irr){  // constructor
		fieldSize = size; logBase = base; irreducible = irr; 
		alog = new int[fieldSize]; log = new int[fieldSize];
		makeLog();
	}

	int modMultiply(int a, int b, int m){  // multiply based on XOR as addition
		int product = 0;
		for (; b > 0; b >>= 1){
			if ((b & 1) > 0) product ^= a;
			a <<= 1;
			if ((a & fieldSize) > 0) a ^= m;
    		}
		return product;
	}    

	void makeLog(){  // first make all powers and then discrete log
		alog[0] = 1;
		for (int i = 1; i < fieldSize; i++)
			alog[i] = modMultiply(logBase, alog[i - 1], irreducible);
		for (int i = 0; i < fieldSize - 1; i++) log[alog[i]] = i;
  	}

	public int multiply(int a, int b){  // multiplication in GF
    		return (a == 0 || b == 0) ? 0 : alog[(log[a] + log[b]) % (fieldSize - 1)];
  	}

	int multiplicativeInverse(int a){
    		return alog[fieldSize - 1 - log[a]];
  	}
};

class Polynomial{
	int[] coeff = null;  
	// coeff[0] is the constant term, coeff[coeff.length - 1] is the highest power term

	public Polynomial(int length){ coeff = new int[length]; } // constructor

	public Polynomial(String data){ // turn string around as data polynomial
		coeff = new int[data.length()]; 
		for (int i = 0; i < coeff.length; i++) 
			coeff[coeff.length - 1 - i] = data.charAt(i);
	} 

	public int evaluate(int x){  // Horner's algorithm
   		int sum = coeff[coeff.length - 1];
		for (int i = coeff.length - 2; i >= 0; i--) 
			sum = f.multiply(sum, x) ^ coeff[i];
		return sum;
 	} 	

	public void display(String title) { // display with highest power first
		if (coeff.length == 0){ 
			System.out.println(title + " [ ]");
			return;
		}
		System.out.print(title + " [ ");
		for (int i = coeff.length - 1; i > 0; i--) 
			System.out.print(Integer.toHexString(coeff[i]) + " ");
		System.out.println(Integer.toHexString(coeff[0]) + " ]");
  	}

	public Polynomial scale(int a){ // ap(x)
		Polynomial newp = new Polynomial(coeff.length);
		for (int i = 0; i < coeff.length; i++) 
			newp.coeff[i] = f.multiply(coeff[i], a);
     		return newp;
  	}

	public Polynomial shift(int r){ // x^r p(x)
		Polynomial newp = new Polynomial(coeff.length + r);
		for (int i = 0; i < coeff.length; i++) newp.coeff[i + r] = coeff[i];
		for (int i = 0; i < r; i++) newp.coeff[i] = 0;
     		return newp;
  	}

	public Polynomial add(Polynomial p2){ // p(x) + p2(x)
		if (coeff.length >= p2.coeff.length){ 
			Polynomial newp = new Polynomial(coeff.length);
			for (int i = 0; i < p2.coeff.length; i++) 
				newp.coeff[i] = coeff[i] ^ p2.coeff[i];
			for (int i = p2.coeff.length; i < coeff.length; i++) 
				newp.coeff[i] = coeff[i];
     				return newp;
		}else{
			Polynomial newp = new Polynomial(p2.coeff.length);
			for (int i = 0; i < coeff.length; i++) 
				newp.coeff[i] = coeff[i] ^ p2.coeff[i];
			for (int i = coeff.length; i < p2.coeff.length; i++) 
				newp.coeff[i] = p2.coeff[i];
     				return newp;
		}
  	}

	public Polynomial RSencode(){ // shift, mod G and add remainder
		Polynomial tmp = shift(Gdegree);
		int head = tmp.coeff.length - 1;
		for (int i = tmp.coeff.length - G.coeff.length; i >= 0; i--)
			tmp = tmp.add(G.scale(tmp.coeff[head--]).shift(i)); 
		Polynomial ret = shift(Gdegree);
		for (int i = 0; i < Gdegree; i++) ret.coeff[i] = tmp.coeff[i];
		return ret;
	}

	public Polynomial computeSyndrome(){
		Polynomial S = new Polynomial(Gdegree);
		for (int i = 0; i < Gdegree; i++) 
			S.coeff[i] = evaluate(f.alog[i]);
		return S;
	}

	Polynomial addError(HashMap<Integer, Integer> errors){  
		// used on result of RSencode to get errorAdded
		// used on errorAdded to get result of RSencode
		int numberOfCodewords = coeff.length;
		Polynomial errorAdded = new Polynomial(numberOfCodewords);
		for (int i = 0; i < numberOfCodewords; i++) 
			errorAdded.coeff[i] = coeff[i];
		errors.forEach((k,v)->{ errorAdded.coeff[k] ^= v; });
		return errorAdded;
	}
};

public DE13A(String[] args){
	M = new Polynomial(args[0]);
}

 Polynomial makeRSG(){
	Polynomial G = new Polynomial(2);
	G.coeff[0] = G.coeff[1] = 1;
	for (int i = 1; i < Gdegree; i++) G = G.shift(1).add(G.scale(f.alog[i]));
	return G;
 }

 void encode(){
	M.display("the message ");
	C = M.RSencode();
	C.display("RS encoded ");
 }

 void randomErrors(){  // used on result of RSencode
	int numberOfPositions = C.coeff.length;
	Random random = new Random();
	while (errors.size() < numberOfErrors){ // random error positions
		int position = random.nextInt(numberOfPositions);
		if (!errors.containsKey(position))
			errors.put(position, 1 + random.nextInt(f.fieldSize - 1));
	}
 }

 void checkErrors(HashMap<Integer, Integer> e){  // print for errors and recoveredErrors
	e.forEach((k, v) -> System.out.println(k + " " + Integer.toHexString(v)));
	System.out.println();
 }

 void errorCorrectionExperiment(){
	randomErrors();
	checkErrors(errors);
	CplusE = C.addError(errors);
	CplusE.display("Error added ");
	Syndrome = CplusE.computeSyndrome();
	Syndrome.display("Syndrome ");
	// Your code to find error location and magnitude

	int s0 = Syndrome.coeff[0]; // S0 = C'(1) = e
	int s1 = Syndrome.coeff[1]; // S1 = C'(α) = e * α^k

	int magnitude = s0;                 // e = S0
	int invS0 = f.multiplicativeInverse(s0);
	int ratio = f.multiply(s1, invS0);  // ratio = α^k

	int location = f.log[ratio];        // k = log_α(ratio)

	System.out.println("Recovered (location, magnitude): " + location + " " + Integer.toHexString(magnitude));

	// Apply correction (XOR same error value at that position)
	HashMap<Integer, Integer> recovered = new HashMap<>();
	recovered.put(location, magnitude);

	Polynomial corrected = CplusE.addError(recovered);
	corrected.display("Corrected codeword ");

 }

public static void main(String[] args){
   if (args.length < 1){
     System.err.println("Usage: java DE13A message");
     return;
   }
   DE13A de13 = new DE13A(args);
   de13.encode();
   de13.errorCorrectionExperiment();
}
}
