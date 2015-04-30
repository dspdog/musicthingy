package com.meerkos;


import com.musicg.graphic.GraphicRender;
import com.musicg.wave.Wave;
import com.musicg.wave.extension.Spectrogram;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;

public class Main {
    public static void main(String[] args) {

        String filename = "audio/random.wav";
        String outFolder = "img";

        // create a wave object
        Wave wave = new Wave(filename);
        Spectrogram spectrogram1 = new Spectrogram(wave, 1024, 2);

        double [][] spec1Data = spectrogram1.getNormalizedSpectrogramData();
        GraphicRender render = new GraphicRender();
        render.renderSpectrogramData(spec1Data,outFolder+ "/sine.jpg");

        SoundThread thread = new SoundThread();
        thread.start();
    }
}


class SoundThread extends Thread {//originally http://www.wolinlabs.com/blog/java.sine.wave.html

    final static public int SAMPLING_RATE = 44100;
    final static public int SAMPLE_SIZE = 2;                 //Sample size in bytes

    final static public double BUFFER_DURATION = 0.0100;      //About a 10ms buffer

    // Size in bytes of sine wave samples we'll create on each loop pass
    final static public int SINE_PACKET_SIZE = (int)(BUFFER_DURATION*SAMPLING_RATE*SAMPLE_SIZE);

    SourceDataLine line;
    public double fFreq;                                    //Set from the pitch slider
    public boolean running = true;


    //Get the number of queued samples in the SourceDataLine buffer
    private int getLineSampleCount() {
        return line.getBufferSize() - line.available();
    }


    //Continually fill the audio output buffer whenever it starts to get empty, SINE_PACKET_SIZE/2
    //samples at a time, until we tell the thread to exit
    public void run() {
        //Position through the sine wave as a percentage (i.e. 0-1 is 0-2*PI)
        double fCyclePosition = 0;

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

        //On each pass main loop fills the available free space in the audio buffer
        //Main loop creates audio samples for sine wave, runs until we tell the thread to exit
        //Each sample is spaced 1/SAMPLING_RATE apart in time
        while (running) {
            fFreq = 200;// + Math.sin((System.currentTimeMillis()%100000)/10f)*50f;

            double fCycleInc = fFreq/SAMPLING_RATE;   //Fraction of cycle between samples

            cBuf.clear();                             //Toss out samples from previous pass

            //Generate SINE_PACKET_SIZE samples based on the current fCycleInc from fFreq
            for (int i=0; i < SINE_PACKET_SIZE/SAMPLE_SIZE; i++) {
                cBuf.putShort((short)(Short.MAX_VALUE * Math.sin(2*Math.PI * fCyclePosition)));

                fCyclePosition += fCycleInc;
                if (fCyclePosition > 1)
                    fCyclePosition -= 1;
            }

            //Write sine samples to the line buffer
            // If the audio buffer is full, this would block until there is enough room,
            // but we are not writing unless we know there is enough space.
            line.write(cBuf.array(), 0, cBuf.position());


            //Wait here until there are less than SINE_PACKET_SIZE samples in the buffer
            //(Buffer size is 2*SINE_PACKET_SIZE at least, so there will be room for
            // at least SINE_PACKET_SIZE samples when this is true)
            try {
                while (getLineSampleCount() > SINE_PACKET_SIZE)
                    Thread.sleep(1);                          // Give UI a chance to run
            }
            catch (InterruptedException e) {                // We don't care about this
            }
        }

        line.drain();
        line.close();
    }
}
