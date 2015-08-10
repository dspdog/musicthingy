package com.meerkos;

import com.meerkos.utils.SimplexNoise;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class SoundThread extends Panel {

    public BufferedImage render;
    public Graphics2D rg;

    final UIThread game = new UIThread();;

    public boolean quit = false;

    public Shape waveform = new Rectangle();
    public Shape phaseWaveform = new Rectangle();

    static final int screenwidth = 512;
    static final int screenheight = 512;

    public static void main(String[] args) {
        JFrame f = new JFrame();

        f.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.exit(0);
            };
        });

        final SoundThread is = new SoundThread();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(), is);

        splitPane.setDividerLocation(32);

        f.add(splitPane);
        f.pack();
        f.setSize(SoundThread.screenwidth, SoundThread.screenheight + 20); // add 20, seems enough for the Frame title,
        f.show();

        is.start();
        System.setProperty("sun.java2d.opengl","True");
        try {
            is.sound();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void start(){
        render =  new BufferedImage(screenwidth, screenheight, BufferedImage.TYPE_INT_RGB); //createImage(screenwidth, screenheight);
        rg = (Graphics2D)render.getGraphics();
        game.start();
    }

    public void update(Graphics gr){
        paint((Graphics2D)gr);
    }

    public void paint(Graphics2D gr) {
        rg = (Graphics2D) render.getGraphics();
        rg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        rg.setColor(Color.black);
        rg.fill(new Rectangle(0,0,screenwidth,screenheight));

        rg.setColor(Color.white);
        rg.setTransform(AffineTransform.getTranslateInstance(0,128));
        rg.draw(waveform);
        rg.setTransform(AffineTransform.getTranslateInstance(0,128*3));
        rg.draw(phaseWaveform);
        gr.drawImage(render, 0, 0, screenwidth, screenheight, this);
    }

    private static void play(SourceDataLine line, Note note, int ms) {
        ms = Math.min(ms, Note.MILLISECONDS);
        int length = Note.SAMPLE_RATE * ms / 1000;
        int count = line.write(note.getData(), 0, length);
    }

    public void sound() throws LineUnavailableException {
        final AudioFormat af =
                new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, true);
        SourceDataLine line = AudioSystem.getSourceDataLine(af);

        line.open(af, Note.SAMPLE_RATE);
        line.start();

            System.out.println("playing...");

            for(int i=0; i<1000; i++){
                System.out.println("note");
                Note n = new Note(i%12+1, new SoundFunction(), 500);
                waveform = n.myPeriodic.getAmplitudesShape();
                phaseWaveform = n.myPeriodic.getPhaseShape();
                play(line,n,500);

            }

        line.drain();
        line.close();
    }

    public class UIThread extends Thread{
        public void run(){
            while(!quit){
                try {
                    repaint();
                    sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public UIThread(){
        }
    }
}



class Note {

    public static final int SAMPLE_RATE = 32 * 1024; // ~16KHz
    public static final int MILLISECONDS = 1000;
    public byte[] data = new byte[MILLISECONDS * SAMPLE_RATE / 1000];
    public double[] dataD = new double[MILLISECONDS * SAMPLE_RATE / 1000];
    public SoundFunction myPeriodic;

    public int myDur =0;
    public int dataLen=0;

    Note(double n, SoundFunction p, int dur) {
        myPeriodic=p;
        myDur=dur;
        dataLen= dur * SAMPLE_RATE / 1000;
        initAsFuncWNoteNumber(n);
    }

    public void initAsFuncWNoteNumber(double noteNumber){ //@ integers REST, A4, A4$, B4, C4, C4$, D4, D4$, E4, F4, F4$, G4, G4$, A5;
        if (noteNumber > 0) initAsFuncWFreq(noteNum2Hz(noteNumber));
    }

    public void initAsFuncWFreq(double freq){
        double time = 0f;
        int len = Math.min(data.length, dataLen);
        for (int i = 0; i < len; i++) {
            time+=1f/(SAMPLE_RATE);
            dataD[i] = myPeriodic.myFunction(time, freq);
        }
    }

    public byte[] getData(){
        double dataMax = 0;
        double dataMin = 9999999;//
        for (int i = 0; i < dataLen; i++){dataMax = Math.max(dataD[i],dataMax);dataMin = Math.min(dataD[i], dataMin);}
        for (int i = 0; i < dataLen; i++)data[i] = (byte)((dataD[i]-dataMin)/(dataMax-dataMin) * 127f);
        return data;
    }

    public double noteNum2Hz(double n){
        double exp = ((double) n - 1) / 12d;
        return 440d * Math.pow(2d, exp);
    }
}



class SoundFunction { //TODO MEMOIZE
    final int NUM_BUCKETS= 256;
    final double[] amplitudes = new double[NUM_BUCKETS];
    final double[] phases = new double[NUM_BUCKETS];

    public SoundFunction(){

        double scale1 = 64 * (Math.random());
        double scale2 = 64 * (Math.random());

        //double scale3 = 32 * (Math.random());
        //double scale4 = 32 * (Math.random());

        double scale1d = 64 * (Math.random());
        double scale2d = 64 * (Math.random());

        //double scale3d = 32 * (Math.random());
        //double scale4d = 32 * (Math.random());

        for(float i=0; i< NUM_BUCKETS; i++){

            float scale = i/NUM_BUCKETS;

            amplitudes[(int)i] = octaveNoise(scale1 * scale + scale1d, scale2 * scale + scale2d);
           // phases[(int)i] = octaveNoise(scale3 * scale + scale3d, scale4 * scale + scale4d);// * 2 * Math.PI;//Math.random()*2*Math.PI;
            //amplitudes[(int)i] =  (Math.random()-0.5);
            phases[(int)i] = (Math.random()-0.5)*2*Math.PI;
        }
    }

    public double octaveNoise(double x, double y){

        int octaves = 3;
        double res=0;
        double div=1;

        for(int i=0; i<octaves; i++){
            res+=SimplexNoise.noise(x/div/div+(i*7%32),y/div/div+(i*7%32))*div;
            div*=2;
        }

        return res;
    }

    public Shape getAmplitudesShape(){
        Polygon p = new Polygon();

        p.addPoint(0,100);
        for(int i=0; i<NUM_BUCKETS; i++){
            p.addPoint((int)(512f*i/NUM_BUCKETS),(int)(amplitudes[i]*10));
        }
        p.addPoint(512,100);

        return p;
    }

    public Shape getPhaseShape(){
        Polygon p = new Polygon();

        p.addPoint(0,100);
        for(int i=0; i<NUM_BUCKETS; i++){
            p.addPoint((int)(512f*i/NUM_BUCKETS),(int)(phases[i]*10));
        }
        p.addPoint(512,100);

        return p;
    }

    public double myFunction(double time, double freq){
        double res = 0;

        for(int i=1; i<NUM_BUCKETS; i++){
            double _period = 1d * NUM_BUCKETS / i ;
            double _angleForThisFreq = 2.0 * Math.PI * time / _period + phases[i];
            res+=amplitudes[i]*Math.sin(freq*_angleForThisFreq);
        }

        return res;
    }
}