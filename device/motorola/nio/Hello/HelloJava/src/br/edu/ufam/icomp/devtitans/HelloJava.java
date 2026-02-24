package br.edu.ufam.icomp.devtitans;

import android.util.Log;

class HelloJava {
    public static final String TAG = "DevTITANS.HelloJava";

    void printHello() {
        System.out.println("Hello World in Java!");
        Log.v(TAG, "Hello World in Java (LogCat)!");
    }

    public static void main(String args[]) {
        HelloJava hello = new HelloJava();
        hello.printHello();
    }
}
