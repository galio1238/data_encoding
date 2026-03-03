// DE8A.java CS5125/6025 Cheng 2025
// ECDSA
// Usage: java DE8A message

import java.math.*;
import java.io.*;
import java.util.*;
import java.security.*;

class Point{
  public BigInteger x; 
  public BigInteger y;
  static Point O = new Point(null, null);
  public Point(BigInteger xx, BigInteger yy){ x = xx; y = yy; }
  public String toString(){
    return this.equals(O) ? "O" :
    "(" + x.toString(16) + ",\n" + y.toString(16) + ")";
  }
}

public class DE8A{

  static BigInteger three = new BigInteger("3");  
  static final int privateKeySize = 255;
  BigInteger p; // modulus
  Point G; // base point
  BigInteger a;  // curve parameter
  BigInteger b;  // curve parameter
  BigInteger n;  // order of G
  Random random = new Random();
  BigInteger e; // message digest
  Point Q; // public key of signatory
  BigInteger r; // signature of e is (r, s)
  BigInteger s; 

void readCurveSpecs(String filename){
    Scanner in = null;
    try {
     in = new Scanner(new File(filename));
    } catch (FileNotFoundException e){
      System.err.println(filename + " not found");
      System.exit(1);
    }
    p = new BigInteger(in.nextLine(), 16);
    n = new BigInteger(in.nextLine(), 16);
    a = new BigInteger(in.nextLine(), 16);
    b = new BigInteger(in.nextLine(), 16 );
    G = new Point(new BigInteger(in.nextLine(), 16), new BigInteger(in.nextLine(), 16));
    in.close();
}

Point add(Point P1, Point P2){
   if (P1.equals(Point.O)) return P2;
   if (P2.equals(Point.O)) return P1;
   if (P1.x.equals(P2.x)) if (P1.y.equals(P2.y)) return selfAdd(P1);
                          else return Point.O;
   BigInteger t1 = P1.x.subtract(P2.x).mod(p);
   BigInteger t2 = P1.y.subtract(P2.y).mod(p);
   BigInteger k = t2.multiply(t1.modInverse(p)).mod(p); // slope
   t1 = k.multiply(k).subtract(P1.x).subtract(P2.x).mod(p); // x3
   t2 = P1.x.subtract(t1).multiply(k).subtract(P1.y).mod(p); // y3
   return new Point(t1,t2);
}   

Point selfAdd(Point P){
     if (P.equals(Point.O)) return Point.O; // O+O=O
     if (P.y.equals(BigInteger.ZERO)) return Point.O;
     BigInteger t1 = P.y.add(P.y).mod(p);  // 2y
     BigInteger t2 = P.x.multiply(P.x).mod(p).multiply(three).add(a).mod(p); // 3xx+a
     BigInteger k = t2.multiply(t1.modInverse(p)).mod(p); // slope or tangent
     t1 = k.multiply(k).subtract(P.x).subtract(P.x).mod(p); // x3 = kk-x-x
     t2 = P.x.subtract(t1).multiply(k).subtract(P.y).mod(p); // y3 = k(x-x3)-y
     return new Point(t1,t2);
}

Point multiply(Point P, BigInteger n){
     if(n.equals(BigInteger.ZERO)) return Point.O;  
     int len = n.bitLength();  // position preceding the most significant bit 1
     Point product = P;
     for(int i = len - 2; i >= 0; i--){
        product = selfAdd(product); 
        if (n.testBit(i)) product = add(product, P);
     }
     return product;
  }

void getMessage(String message){
   MessageDigest md = null;
   try {
        md = MessageDigest.getInstance("SHA-256");
   } catch (NoSuchAlgorithmException e){
	System.err.println(e.getMessage());
	System.exit(1);
   }
     md.update(message.getBytes());
     e = new BigInteger(md.digest());
     System.out.println("e: " + e.toString(16));
}

 void generateSignature(){
    BigInteger d = new BigInteger(privateKeySize, random);
    Q = multiply(G, d);
    System.out.println("Q: " + Q.toString());
    BigInteger k = new BigInteger(privateKeySize, random);
    Point R = multiply(G, k);
    r = R.x.mod(n);
    System.out.println("r: " + r.toString(16));
    BigInteger kinverse = k.modInverse(n);
    s = kinverse.multiply(e.add(d.multiply(r)).mod(n)).mod(n);
    System.out.println("s: " + s.toString(16));
 }

 void verifySignature(){
    BigInteger w = s.modInverse(n); // s^-1 mod n
    BigInteger u1 = e.multiply(w).mod(n);// ew
    BigInteger u2 = r.multiply(w).mod(n);// rw
    Point X = add(multiply(G, u1), multiply(Q, u2)); // (x1, x2) = u1G + u2Q
    BigInteger v = X.x.mod(n); // x1 mod n
    System.out.println("v: " + v.toString(16));
 }

 public static void main(String[] args){
    DE8A de8 = new DE8A();
    de8.getMessage(args[0]);
    de8.readCurveSpecs("ECP256.txt");
    de8.generateSignature();
    de8.verifySignature();
 }
}

