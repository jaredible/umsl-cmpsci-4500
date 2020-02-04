/*******************************************************************************
 * Copyright (c) 2020 Jared Diehl. All rights reserved.
 *******************************************************************************/

package main;

import java.util.Random;
import java.util.Scanner;

public class Simulation {

  public static final int DIMENSION_MIN = 2;
  public static final int DIMENSION_MAX = 50;
  public static final int UPDATES_MAX = 1000000;

  private final Random random = new Random();
  private final Forest forest;
  private boolean running = false;
  private int updates = 0;

  public Simulation(int a, int b) {
    forest = new Forest(a, b);

    forest.getPerson1().setLocation(0, 0);
    forest.getPerson2().setLocation(a - 1, b - 1);
  }

  public void start() {
    running = true;

    run();
  }

  public void stop() {
    running = false;
  }

  private void run() {
    while (running) {
      update();
    }
  }

  private void update() {
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
      return;
    }

    updates++;
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
    int a = prompt(scanner, String.format("Please enter an integer value for A [%d, %d]: ",
        DIMENSION_MIN, DIMENSION_MAX), error);
    int b = prompt(scanner, String.format("Please enter an integer value for B [%d, %d]: ",
        DIMENSION_MIN, DIMENSION_MAX), error);
    scanner.close();

    Simulation simulation = new Simulation(a, b);
    simulation.start();
    System.out.println(simulation);
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
      return String.format("[width: %d, height: %d, person1: %s, person2: %s]", width, height,
          person1, person2);
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
