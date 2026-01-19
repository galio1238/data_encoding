// DE2B.java CS5125 cheng 2026
// implements the C code in rfc 1952 make_crcv_table() in Java
// with minimum scopes for all local variables
// print out the first 10 table items in hexadecimal.
// correct if the same as the first 10 constants in table[] of DE2A.java
// Usage: java DE2B

public class DE2B {

    public static void main(String[] args) {

        for (int n = 0; n < 10; n++){ // only compute the first 10 table items
            int c = n;
            for (int k = 0; k < 8; k++){
                // your Java code corresponding to the C code
                if ((c & 1) != 0) {
                   c = 0xEDB88320 ^ (c >>> 1);
                } else {
                   c = c >>> 1;
                }
                // You may not need {, }, or L in your Java code
            }
            System.out.println(Integer.toHexString(c));
        }
    }
}

