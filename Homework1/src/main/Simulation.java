/*******************************************************************************
 * Copyright (c) 2020 Jared Diehl. All rights reserved.
 *******************************************************************************/

package main;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Random;
import java.util.Scanner;
import javax.swing.JFrame;

public class Simulation extends Canvas implements Runnable {

  private static final long serialVersionUID = 1L;

  public static final int DIMENSION_MIN = 2;
  public static final int DIMENSION_MAX = 50;
  public static final int UPDATES_MAX = 1000000;

  private final Random random = new Random();
  private final Forest forest;
  private boolean running = false;
  private int updates = 0;

  private static final int SCALE = 10;
  private final BufferedImage image;
  private final int[] pixels;

  public Simulation(int a, int b) {
    forest = new Forest(a, b);

    forest.getPerson1().setLocation(0, 0);
    forest.getPerson2().setLocation(a - 1, b - 1);

    image = new BufferedImage(a, b, BufferedImage.TYPE_INT_RGB);
    pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
  }

  public void start() {
    running = true;

    new Thread(this).start();
  }

  public void stop() {
    running = false;
  }

  private void init() {}

  public void run() {
    long lastTime = System.nanoTime();
    double unprocessed = 0;
    double nsPerUpdate = 1000000000.0 / 60;
    int updates = 0;
    int frames = 0;
    long lastTimer = System.currentTimeMillis();

    init();

    while (running) {
      long now = System.nanoTime();
      unprocessed += (now - lastTime) / nsPerUpdate;
      lastTime = now;

      while (unprocessed >= 1) {
        updates++;
        update();
        unprocessed--;
      }

      try {
        Thread.sleep(2);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      frames++;
      draw();

      if (System.currentTimeMillis() - lastTimer > 1000) {
        lastTimer -= 1000;
        System.out.println(String.format("updates: %d, frames: %d", updates, frames));
        updates = 0;
        frames = 0;
      }
    }
  }

  private void update() {
    updates++;

    int person1MotionX = 0;
    int person1MotionY = 0;
    int person2MotionX = 0;
    int person2MotionY = 0;

    if (random.nextBoolean()) {
      person1MotionX = random.nextInt(2 + 1) - 1;
    } else {
      person1MotionY = random.nextInt(2 + 1) - 1;
    }

    if (random.nextBoolean()) {
      person2MotionX = random.nextInt(2 + 1) - 1;
    } else {
      person2MotionY = random.nextInt(2 + 1) - 1;
    }

    forest.getPerson1().tryMove(person1MotionX, person1MotionY);
    forest.getPerson2().tryMove(person2MotionX, person2MotionY);

    boolean shouldStop = forest.getPerson1().with(forest.getPerson2()) || updates > UPDATES_MAX;

    if (shouldStop) {
      stop();
    }
  }

  private void draw() {
    BufferStrategy bs = getBufferStrategy();
    if (bs == null) {
      createBufferStrategy(3);
      requestFocus();
      return;
    }

    for (int i = 0; i < pixels.length; i++) {
      pixels[i] = 0;
    }

    pixels[forest.getPerson1().getX() + forest.getPerson1().getY() * forest.getWidth()] = 0xff9bdc;
    pixels[forest.getPerson2().getX() + forest.getPerson2().getY() * forest.getWidth()] = 0x009bff;

    Graphics g = bs.getDrawGraphics();
    g.fillRect(0, 0, getWidth(), getHeight());

    int ww = forest.getWidth() * SCALE;
    int hh = forest.getHeight() * SCALE;
    int xo = (getWidth() - ww) / 2;
    int yo = (getHeight() - hh) / 2;
    g.drawImage(image, xo, yo, ww, hh, null);
    g.dispose();
    bs.show();
  }

  public boolean isRunning() {
    return running;
  }

  public int getUpdates() {
    return updates;
  }

  public String toString() {
    return String.format("[running: %b, updates: %d, forest: %s]", running, updates, forest);
  }

  private static int prompt(Scanner scanner, String message, String error) {
    int result = -1;

    do {
      System.out.println(message);
      while (!scanner.hasNextInt()) {
        System.out.println(error);
        scanner.next();
      }
      result = scanner.nextInt();
    } while (!(result >= DIMENSION_MIN && result <= DIMENSION_MAX));

    return result;
  }

  public static void main(String[] args) {
    String error = "Please enter an integer!";
    Scanner scanner = new Scanner(System.in);
    int a = prompt(scanner, String.format("Please enter an integer value for A [%d, %d]: ", DIMENSION_MIN, DIMENSION_MAX), error);
    int b = prompt(scanner, String.format("Please enter an integer value for B [%d, %d]: ", DIMENSION_MIN, DIMENSION_MAX), error);
    scanner.close();

    Simulation simulation = new Simulation(a, b);
    simulation.setMinimumSize(new Dimension(a * SCALE, b * SCALE));
    simulation.setMaximumSize(new Dimension(a * SCALE, b * SCALE));
    simulation.setPreferredSize(new Dimension(a * SCALE, b * SCALE));

    JFrame frame = new JFrame(String.format("Simulation [%d, %d]", a, b));
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(simulation, BorderLayout.CENTER);
    frame.pack();
    frame.setResizable(false);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);

    simulation.start();
  }

  private class Forest {

    private final int width;
    private final int height;
    private final Person person1 = new Person(this);
    private final Person person2 = new Person(this);

    public Forest(int width, int height) {
      this.width = width;
      this.height = height;
    }

    public boolean mayPass(int x, int y) {
      return x >= 0 && y >= 0 && x < width && y < width;
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
    }

    public Person getPerson1() {
      return person1;
    }

    public Person getPerson2() {
      return person2;
    }

    public String toString() {
      return String.format("[width: %d, height: %d, person1: %s, person2: %s]", width, height, person1, person2);
    }

  }

  private class Person {

    private final Forest forest;
    private int x;
    private int y;

    public Person(Forest forest) {
      this.forest = forest;
    }

    public void tryMove(int motionX, int motionY) {
      if (motionX != 0 && motionY != 0)
        throw new IllegalArgumentException("Can only move along one axis at a time!");

      int newX = x + motionX;
      int newY = y + motionY;

      if (forest.mayPass(newX, newY)) {
        x += motionX;
        y += motionY;
      }
    }

    public boolean with(Person person) {
      return x == person.getX() && y == person.getY();
    }

    public void setLocation(int x, int y) {
      this.x = x;
      this.y = y;
    }

    public int getX() {
      return x;
    }

    public int getY() {
      return y;
    }

    public String toString() {
      return String.format("[x: %d, y: %d]", x, y);
    }

  }

}
