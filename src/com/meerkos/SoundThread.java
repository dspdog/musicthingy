package com.meerkos;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;

/**
 * Created by user on 5/3/2015.
 */
class SoundThread extends Thread {//originally http://www.wolinlabs.com/blog/java.sine.wave.html

    final static public int SAMPLING_RATE = 44100;
    final static public int SAMPLE_SIZE = 2;                 //Sample size in bytes

    final static public double BUFFER_DURATION = 0.0100;      //About a 10ms buffer

    // Size in bytes of sine wave samples we'll create on each loop pass
    final static public int SINE_PACKET_SIZE = (int)(BUFFER_DURATION*SAMPLING_RATE*SAMPLE_SIZE);

    final static public int SAMPLES_PER_LOOP = (int)(BUFFER_DURATION*SAMPLING_RATE);


    SourceDataLine line;
    public double fFreq;                                    //Set from the pitch slider
    public boolean running = true;

    //Get the number of queued samples in the SourceDataLine buffer
    private int getLineSampleCount() {
        return line.getBufferSize() - line.available();
    }


    public double [][] spectralData;

    public void setSpectralData(double[][] data){
        spectralData =data;
    }

    public double soundFunction(double time){
       double angle = time * Math.PI*2f;
       return  Math.sin(100*angle);
    }

    //Continually fill the audio output buffer whenever it starts to get empty, SINE_PACKET_SIZE/2
    //samples at a time, until we tell the thread to exit
    public void run() {


        //Open up the audio output, using a sampling rate of 44100hz, 16 bit samples, mono, and big
        // endian byte ordering.   Ask for a buffer size of at least 2*SINE_PACKET_SIZE
        try {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format, SINE_PACKET_SIZE*2);

            if (!AudioSystem.isLineSupported(info))
                throw new LineUnavailableException();

            line = (SourceDataLine)AudioSystem.getLine(info);
            line.open(format);
            line.start();
        }
        catch (LineUnavailableException e) {
            System.out.println("Line of that type is not available");
            e.printStackTrace();
            System.exit(-1);
        }

        System.out.println("Requested line buffer size = " + SINE_PACKET_SIZE*2);
        System.out.println("Actual line buffer size = " + line.getBufferSize());


        ByteBuffer cBuf = ByteBuffer.allocate(SINE_PACKET_SIZE);

        while (running) {

            cBuf.clear();                             //Toss out samples from previous pass
            float time=0;
            for (int i=0; i < SAMPLES_PER_LOOP; i++) {
                cBuf.putShort((short)(Short.MAX_VALUE * soundFunction(time)));
                time += 1.0/SAMPLING_RATE;
            }

            line.write(cBuf.array(), 0, cBuf.position());

            try {
                while (getLineSampleCount() > SINE_PACKET_SIZE)
                    Thread.sleep(1);
            }
            catch (InterruptedException e) {
            }
        }

        line.drain();
        line.close();
    }

/*
    public enum Note {//http://stackoverflow.com/questions/2064066/does-java-have-built-in-libraries-for-audio-synthesis/2065693#2065693

        REST, A4, A4$, B4, C4, C4$, D4, D4$, E4, F4, F4$, G4, G4$, A5;
        public static final int SAMPLE_RATE = 32 * 1024; // ~16KHz
        public static final int SECONDS = 2;
        private byte[] sin = new byte[SECONDS * SAMPLE_RATE];

        Note() {
            int n = this.ordinal();
            if (n > 0) {
                double exp = ((double) n - 1) / 12d;
                double f = 440d * Math.pow(2d, exp);
                double period = (double)SAMPLE_RATE / f;
                for (int i = 0; i < sin.length; i++) {
                    sin[i] = (byte)(harmonicSin(2.0 * Math.PI * i / period, 2, -2)/8f * 127f);
                }
            }
        }

        public double harmonicSin(double s, int start, int end){

            return  Math.sin(s) ; //+ Math.sin(s/2) + Math.sin(s/4);
        }

        public byte[] data() {
            return sin;
        }
    }*/
}
