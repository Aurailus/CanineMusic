package com.aurailus.caninemusic.PaletteGrabber;

import java.util.ArrayList;

class Pixel {
  private int color = 0;
  int frequency = 0;
  ArrayList<Double> RGBarr = new ArrayList<>();

  Pixel(int color){
    this.color = color;
    RGBarr.add((double)((color >> 16) & 0x000000FF));
    RGBarr.add((double)((color >> 8) & 0x000000FF));
    RGBarr.add((double)((color) & 0x000000FF));
  }
  boolean compare(int comparison){
    if (comparison == color) {
      frequency += 1;
    }
    return (comparison == color);
  }
  double findEuclideanDist(ArrayList<Double> pix){
    double sum = 0.0;
    for (int i = 0; i < 3; i++){
      sum += Math.pow((RGBarr.get(i) - pix.get(i)), 2.0);
    }
    return Math.sqrt(sum);
  }
  int getColor() {
    return color;
  }
}
