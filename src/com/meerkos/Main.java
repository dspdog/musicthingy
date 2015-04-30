package com.meerkos;


import com.musicg.graphic.GraphicRender;
import com.musicg.wave.Wave;
import com.musicg.wave.extension.Spectrogram;

public class Main {
    public static void main(String[] args) {

        String filename = "audio/pluck.wav";
        String outFolder = "img";

        // create a wave object
        Wave wave = new Wave(filename);
        Spectrogram spectrogram1 = new Spectrogram(wave, 1024, 0);

        double [][] spec1Data = spectrogram1.getNormalizedSpectrogramData();
        GraphicRender render = new GraphicRender();
        render.renderSpectrogramData(spec1Data,outFolder+ "/sine.jpg");


    }
}