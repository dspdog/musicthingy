package com.meerkos;

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
        for(int attempt=0; attempt<10; attempt++){

            ArrayList<Integer> arrpeggioNotes = new ArrayList<Integer>();
            ArrayList<Integer> arrpeggioSustains = new ArrayList<Integer>();

            int numNotes = (int)(Math.random()*10f)+3;
            for(int i=0; i<numNotes; i++){
                arrpeggioNotes.add(new Integer((int) (Math.random() * 12f)));
                arrpeggioSustains.add(new Integer((int) (100 + Math.random() * 200)));
            }

            for(float i=0; i<6; i+=1){
                int noteIndex=0;
                for(Integer _int : arrpeggioNotes){
                    play(line,new Note(_int+i),arrpeggioSustains.get(noteIndex));
                    noteIndex++;
                }
            }
        }

        line.drain();
        line.close();
    }

    private static void play(SourceDataLine line, Note note, int ms) {
        ms = Math.min(ms, Note.SECONDS * 1000);
        int length = Note.SAMPLE_RATE * ms / 1000;
        int count = line.write(note.getData(), 0, length);
    }
}

class Note {

    public static final int SAMPLE_RATE = 16 * 1024 * 4; // ~16KHz * 4
    public static final int SECONDS = 1;
    public byte[] data = new byte[SECONDS * SAMPLE_RATE];
    public double[] dataD = new double[SECONDS * SAMPLE_RATE];

    Note(double n) {
        initAsFuncWNoteNumber(n);
    }

    public void initAsFuncWNoteNumber(double noteNumber){ //@ integers REST, A4, A4$, B4, C4, C4$, D4, D4$, E4, F4, F4$, G4, G4$, A5;
        if (noteNumber > 0) initAsFuncWFreq(noteNum2Hz(noteNumber));
    }

    public void initAsFuncWFreq(double freq){
        for (int i = 0; i < data.length; i++) {
            double period = (double)SAMPLE_RATE / freq;
            double angle = 2.0 * Math.PI * i / period;
            dataD[i] = periodicFunction(angle);
        }
    }

    public byte[] getData(){
        double dataMax = 0;
        for (int i = 0; i < data.length; i++)dataMax = Math.max(dataD[i],dataMax);
        for (int i = 0; i < data.length; i++)data[i] = (byte)(dataD[i]/dataMax * 127f);
        return data;
    }

    public double periodicFunction(double angle){
        return Math.sin(angle) + Math.sin(angle/2)+ Math.sin(angle/4)+ Math.sin(angle/8);
    }

    public double noteNum2Hz(double n){
        double exp = ((double) n - 1) / 12d;
        return 440d * Math.pow(2d, exp);
    }
}
