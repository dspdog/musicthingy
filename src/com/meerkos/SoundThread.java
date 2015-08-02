package com.meerkos;

import com.meerkos.utils.SimplexNoise;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;

public class SoundThread {

    public static void main(String[] args) throws LineUnavailableException {
        final AudioFormat af =
            new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, true);
        SourceDataLine line = AudioSystem.getSourceDataLine(af);
        line.open(af, Note.SAMPLE_RATE);
        line.start();

        //TODO - RANDOM TIMBRES?

        //RANDOM ARPEGGIOS
        for(int attempt=0; attempt<100; attempt++){

            ArrayList<Integer> arrpeggioNotes = new ArrayList<Integer>();
            ArrayList<Integer> arrpeggioSustains = new ArrayList<Integer>();

            int numNotes = 3;
            for(int i=0; i<numNotes; i++){
                arrpeggioNotes.add(new Integer((int) (Math.random() * 12f)));
                arrpeggioSustains.add(1000 /*new Integer((int) (400 + Math.random() * 800))*/);
            }

            SoundFunction p = new SoundFunction();
            System.out.println("generating sound...");
            ArrayList<Note> notes = new ArrayList<Note>();
            for(float i=0; i<2; i+=1){ //arrpegg repeater

                int noteIndex=0;
                for(Integer _int : arrpeggioNotes){

                    p = new SoundFunction();

                    notes.add(new Note(_int + i, p)); // play(line, , arrpeggioSustains.get(noteIndex));

                    noteIndex++;
                    System.out.println("note " + notes.size());
                }
            }

            System.out.println("playing...");

            int noteIndexGlobal=0;
            for(float i=0; i<2; i+=1){
                p = new SoundFunction();
                int noteIndex=0;
                for(Integer _int : arrpeggioNotes){
                    play(line,notes.get(noteIndexGlobal),arrpeggioSustains.get(noteIndex));
                    noteIndex++;
                    noteIndexGlobal++;
                }
            }
        }

        line.drain();
        line.close();
    }

    private static void play(SourceDataLine line, Note note, int ms) {
        ms = Math.min(ms, Note.MILLISECONDS);
        int length = Note.SAMPLE_RATE * ms / 1000;
        int count = line.write(note.getData(), 0, length);
    }
}

class Note {

    public static final int SAMPLE_RATE = 16 * 1024; // ~16KHz
    public static final int MILLISECONDS = 1000;
    public byte[] data = new byte[MILLISECONDS * SAMPLE_RATE / 1000];
    public double[] dataD = new double[MILLISECONDS * SAMPLE_RATE / 1000];
    public SoundFunction myPeriodic;

    Note(double n, SoundFunction p) {
        myPeriodic=p;
        initAsFuncWNoteNumber(n);
    }

    public void initAsFuncWNoteNumber(double noteNumber){ //@ integers REST, A4, A4$, B4, C4, C4$, D4, D4$, E4, F4, F4$, G4, G4$, A5;
        if (noteNumber > 0) initAsFuncWFreq(noteNum2Hz(noteNumber));
    }

    public void initAsFuncWFreq(double freq){
        double time = 0f;
        for (int i = 0; i < data.length; i++) {
            time+=1f/(SAMPLE_RATE);
            dataD[i] = myPeriodic.myFunction(time, freq);
        }
    }

    public byte[] getData(){
        double dataMax = 0;
        double dataMin = 9999999;
        for (int i = 0; i < data.length; i++){dataMax = Math.max(dataD[i],dataMax);dataMin = Math.min(dataD[i], dataMin);}
        for (int i = 0; i < data.length; i++)data[i] = (byte)((dataD[i]-dataMin)/(dataMax-dataMin) * 127f);
        return data;
    }

    public double noteNum2Hz(double n){
        double exp = ((double) n - 1) / 12d;
        return 440d * Math.pow(2d, exp);
    }
}

class SoundFunction { //TODO MEMOIZE
    final int NUM_BUCKETS= 64;
    final double[] amplitudes = new double[NUM_BUCKETS];
    final double[] phases = new double[NUM_BUCKETS];

    public SoundFunction(){

        double scale1 = 64 * (Math.random()-0.5);
        double scale2 = 64 * (Math.random()-0.5);

        double scale3 = 64 * (Math.random()-0.5);
        double scale4 = 64 * (Math.random()-0.5);

        for(float i=0; i< phases.length; i++){
            amplitudes[(int)i] =(Math.random()-0.5);//SimplexNoise.noise(scale1 * i / phases.length, scale2 * i / phases.length);
            phases[(int)i] = (Math.random()-0.5)*2*Math.PI;
            //phases[(int)i] = SimplexNoise.noise(scale3*i/phases.length, scale4*i/phases.length)*2*Math.PI;//Math.random()*2*Math.PI;
        }
    }

    //TODO "OCTAVE NOISE" function <--reuse dream machine shader?
    //TODO "VIEW THE WAVEFORM" and amplitudes

    public double myFunction(double time, double freq){
        double period = 1f / freq;
        double angle = 2.0 * Math.PI * time / period;

        double res = 0;

        for(int i=0; i<NUM_BUCKETS; i++){
            res+=amplitudes[i]*Math.sin((angle) * i / NUM_BUCKETS + phases[i]);
        }

        //CREATE RANDOM SET OF RATIONALS...
        return res; //Math.sin(angle*2) + 2*Math.sin(angle) + 3*Math.sin(angle/2)+ 3*Math.sin(angle/3)+ 9*Math.sin(angle/5) + 1*Math.sin(angle/4)+ 8*Math.sin(angle/8)+ Math.sin(angle/16);
    }
}