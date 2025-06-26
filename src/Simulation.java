import java.util.ArrayList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.*;

public class Simulation extends JFrame {
	static int sleep = 8; // 8
	static double pix = 0.2;// 0.2
	int anzFz = 120; // number of swarm objects (excluding special vehicle)
	ArrayList<Vehicle> allVehicles = new ArrayList<Vehicle>();
	JPanel canvas = new Canvas(allVehicles, pix);
	JButton redButton;
	JButton yellowButton;
	JButton lavenderButton;
	volatile int specialVehicleType = 0; // 0: none, 1: red, 2: yellow, 3: lavender
	Timer redTimer = new Timer();
	TimerTask redTimerTask = null;
	Timer newVehiclesTimer = new Timer();
	TimerTask newVehiclesTask = null;

	Simulation() {
		setTitle("Swarm");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(null);

		for (int k = 0; k < anzFz; k++) {
			Vehicle car = new Vehicle();
			allVehicles.add(car);
		}

		redButton = new JButton("Start Red Special Vehicle");
		yellowButton = new JButton("Start Yellow Special Vehicle");
		lavenderButton = new JButton("Start Lavender Special Vehicle");
		redButton.setBounds(20, 10, 200, 30);
		yellowButton.setBounds(240, 10, 220, 30);
		lavenderButton.setBounds(480, 10, 240, 30);
		canvas.setBounds(0, 50, 1000, 750);
		add(redButton);
		add(yellowButton);
		add(lavenderButton);
		add(canvas);
		setSize(1020, 850);
		setVisible(true);

		redButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				activateSpecialVehicle(1);
			}
		});
		yellowButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				activateSpecialVehicle(2);
			}
		});
		lavenderButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				activateSpecialVehicle(3);
			}
		});
	}

	private synchronized void activateSpecialVehicle(int type) {
		// Remove any existing special vehicle
		allVehicles.removeIf(v -> v.type == 1 || v.type == 2 || v.type == 3);
		// Cancel any running red timer
		if (redTimerTask != null) {
			redTimerTask.cancel();
			redTimerTask = null;
		}
		// Cancel any pending new vehicles task
		if (newVehiclesTask != null) {
			newVehiclesTask.cancel();
			newVehiclesTask = null;
		}
		// Add the new special vehicle
		Vehicle special = new Vehicle();
		special.type = type;
		if (type == 1) {
			special.max_vel = 1.0;
			special.pos[0] = 1000 * Simulation.pix * Math.random();
			special.pos[1] = 800 * Simulation.pix * Math.random();
			// Start timer to remove after 10 seconds
			redTimerTask = new TimerTask() {
				public void run() {
					synchronized (Simulation.this) {
						allVehicles.removeIf(v -> v.type == 1);
						if (specialVehicleType == 1) specialVehicleType = 0;
						repaint();
						// Schedule new vehicles creation after 3 seconds
						newVehiclesTask = new TimerTask() {
							public void run() {
								synchronized (Simulation.this) {
									int numTarget = 120;
									int numRemaining = 0;
									for (Vehicle veh : allVehicles) {
										if (veh.type == 0) numRemaining++;
									}
									int numToAdd = numTarget - numRemaining;
									if (numToAdd > 0 && numRemaining > 0) {
										ArrayList<Vehicle> newVehicles = new ArrayList<>();
										for (int i = 0; i < numToAdd; i++) {
											// Select two random parents from existing black vehicles
											Vehicle parent1 = allVehicles.get((int)(Math.random() * numRemaining));
											Vehicle parent2 = allVehicles.get((int)(Math.random() * numRemaining));
											Vehicle newVeh = new Vehicle();
											// Inherit and mutate properties
											newVeh.max_vel = (parent1.max_vel + parent2.max_vel) / 2 + (Math.random() - 0.5) * 0.2; // mutation
											newVeh.rad_sep = (parent1.rad_sep + parent2.rad_sep) / 2 + (Math.random() - 0.5) * 2.0;
											newVeh.rad_zus = (parent1.rad_zus + parent2.rad_zus) / 2 + (Math.random() - 0.5) * 5.0;
											// Clamp values to reasonable ranges
											newVeh.max_vel = Math.max(0.2, Math.min(newVeh.max_vel, 2.0));
											newVeh.rad_sep = Math.max(5, Math.min(newVeh.rad_sep, 30));
											newVeh.rad_zus = Math.max(50, Math.min(newVeh.rad_zus, 200));
											// Position and velocity from template
											newVeh.pos[0] = parent1.pos[0];
											newVeh.pos[1] = parent1.pos[1];
											newVeh.vel[0] = parent1.vel[0];
											newVeh.vel[1] = parent1.vel[1];
											// Crossover for weights
											double parent1Cohesion = parent1.getCohesionWeight();
											double parent2Cohesion = parent2.getCohesionWeight();
											double parent1Alignment = parent1.getAlignmentWeight();
											double parent2Alignment = parent2.getAlignmentWeight();
											double parent1Separation = parent1.getSeparationWeight();
											double parent2Separation = parent2.getSeparationWeight();
											double childCohesion = (parent1Cohesion + parent2Cohesion) / 2.0;
											double childAlignment = (parent1Alignment + parent2Alignment) / 2.0;
											double childSeparation = (parent1Separation + parent2Separation) / 2.0;
											System.out.printf("Green Vehicle %d: Parent1 (C=%.4f, A=%.4f, S=%.4f), Parent2 (C=%.4f, A=%.4f, S=%.4f)\n",
												i, parent1Cohesion, parent1Alignment, parent1Separation, parent2Cohesion, parent2Alignment, parent2Separation);
											// Mutation: 10% chance for each weight
											boolean mutated = false;
											if (Math.random() < 0.1) {
												childCohesion = Individual.MIN_WEIGHT + Math.random() * (Individual.MAX_WEIGHT - Individual.MIN_WEIGHT);
												System.out.printf("  Mutation: New cohesionWeight = %.4f\n", childCohesion);
												mutated = true;
											}
											if (Math.random() < 0.1) {
												childAlignment = Individual.MIN_WEIGHT + Math.random() * (Individual.MAX_WEIGHT - Individual.MIN_WEIGHT);
												System.out.printf("  Mutation: New alignmentWeight = %.4f\n", childAlignment);
												mutated = true;
											}
											if (Math.random() < 0.1) {
												childSeparation = Individual.MIN_WEIGHT + Math.random() * (Individual.MAX_WEIGHT - Individual.MIN_WEIGHT);
												System.out.printf("  Mutation: New separationWeight = %.4f\n", childSeparation);
												mutated = true;
											}
											if (!mutated) {
												System.out.println("  Mutation: No mutation occurred for this green vehicle.");
											}
											System.out.printf("  Resulting Green Vehicle: Cohesion=%.4f, Alignment=%.4f, Separation=%.4f\n",
												childCohesion, childAlignment, childSeparation);
											newVeh.setCohesionWeight(childCohesion);
											newVeh.setAlignmentWeight(childAlignment);
											newVeh.setSeparationWeight(childSeparation);
											newVeh.isNew = true;
											newVehicles.add(newVeh);
										}
										allVehicles.addAll(newVehicles);
									}
									repaint();
								}
							}
						};
						newVehiclesTimer.schedule(newVehiclesTask, 3000);
					}
				}
			};
			redTimer.schedule(redTimerTask, 10000);
		} else if (type == 2) {
			special.max_vel = 0.75;
			special.pos[0] = 1000 * Simulation.pix * Math.random();
			special.pos[1] = 800 * Simulation.pix * Math.random();
			// Start timer to remove after 10 seconds
			redTimerTask = new TimerTask() {
				public void run() {
					synchronized (Simulation.this) {
						allVehicles.removeIf(v -> v.type == 2);
						if (specialVehicleType == 2) specialVehicleType = 0;
						repaint();
						// Schedule new orange vehicles creation after 3 seconds
						newVehiclesTask = new TimerTask() {
							public void run() {
								synchronized (Simulation.this) {
									int numTarget = 120;
									int numRemaining = 0;
									for (Vehicle veh : allVehicles) {
										if (veh.type == 0 && !veh.isNew && !veh.isOrange) numRemaining++;
									}
									int numToAdd = 120 - allVehicles.size();
									if (numToAdd > 0 && numRemaining > 0) {
										ArrayList<Vehicle> newVehicles = new ArrayList<>();
										// Collect eligible parents (green vehicles only)
										ArrayList<Vehicle> eligibleParents = new ArrayList<>();
										for (Vehicle veh : allVehicles) {
											if (veh.type == 0 && veh.isNew && !veh.isOrange) eligibleParents.add(veh);
										}
										for (int i = 0; i < numToAdd; i++) {
											// Select two random parents from eligible green vehicles
											Vehicle parent1 = eligibleParents.get((int)(Math.random() * eligibleParents.size()));
											Vehicle parent2 = eligibleParents.get((int)(Math.random() * eligibleParents.size()));
											Vehicle newVeh = new Vehicle();
											// Inherit and mutate properties
											newVeh.max_vel = (parent1.max_vel + parent2.max_vel) / 2 + (Math.random() - 0.5) * 0.2; // mutation
											newVeh.rad_sep = (parent1.rad_sep + parent2.rad_sep) / 2 + (Math.random() - 0.5) * 2.0;
											newVeh.rad_zus = (parent1.rad_zus + parent2.rad_zus) / 2 + (Math.random() - 0.5) * 5.0;
											// Clamp values to reasonable ranges
											newVeh.max_vel = Math.max(0.2, Math.min(newVeh.max_vel, 2.0));
											newVeh.rad_sep = Math.max(5, Math.min(newVeh.rad_sep, 30));
											newVeh.rad_zus = Math.max(50, Math.min(newVeh.rad_zus, 200));
											// Position and velocity from template
											newVeh.pos[0] = parent1.pos[0];
											newVeh.pos[1] = parent1.pos[1];
											newVeh.vel[0] = parent1.vel[0];
											newVeh.vel[1] = parent1.vel[1];
											// Crossover for weights
											double parent1Cohesion = parent1.getCohesionWeight();
											double parent2Cohesion = parent2.getCohesionWeight();
											double parent1Alignment = parent1.getAlignmentWeight();
											double parent2Alignment = parent2.getAlignmentWeight();
											double parent1Separation = parent1.getSeparationWeight();
											double parent2Separation = parent2.getSeparationWeight();
											double childCohesion = (parent1Cohesion + parent2Cohesion) / 2.0;
											double childAlignment = (parent1Alignment + parent2Alignment) / 2.0;
											double childSeparation = (parent1Separation + parent2Separation) / 2.0;
											System.out.printf("Orange Vehicle %d: Parent1 (ID=%d, C=%.4f, A=%.4f, S=%.4f), Parent2 (ID=%d, C=%.4f, A=%.4f, S=%.4f)\n",
												i, parent1.id, parent1Cohesion, parent1Alignment, parent1Separation, parent2.id, parent2Cohesion, parent2Alignment, parent2Separation);
											// Mutation: 10% chance for each weight
											boolean mutated = false;
											if (Math.random() < 0.1) {
												childCohesion = Individual.MIN_WEIGHT + Math.random() * (Individual.MAX_WEIGHT - Individual.MIN_WEIGHT);
												System.out.printf("  Mutation: New cohesionWeight = %.4f\n", childCohesion);
												mutated = true;
											}
											if (Math.random() < 0.1) {
												childAlignment = Individual.MIN_WEIGHT + Math.random() * (Individual.MAX_WEIGHT - Individual.MIN_WEIGHT);
												System.out.printf("  Mutation: New alignmentWeight = %.4f\n", childAlignment);
												mutated = true;
											}
											if (Math.random() < 0.1) {
												childSeparation = Individual.MIN_WEIGHT + Math.random() * (Individual.MAX_WEIGHT - Individual.MIN_WEIGHT);
												System.out.printf("  Mutation: New separationWeight = %.4f\n", childSeparation);
												mutated = true;
											}
											if (!mutated) {
												System.out.println("  Mutation: No mutation occurred for this orange vehicle.");
											}
											System.out.printf("  Resulting Orange Vehicle: Cohesion=%.4f, Alignment=%.4f, Separation=%.4f\n",
												childCohesion, childAlignment, childSeparation);
											newVeh.setCohesionWeight(childCohesion);
											newVeh.setAlignmentWeight(childAlignment);
											newVeh.setSeparationWeight(childSeparation);
											newVeh.isNew = false;
											newVeh.isOrange = true;
											newVehicles.add(newVeh);
										}
										allVehicles.addAll(newVehicles);
									}
									repaint();
								}
							}
						};
						newVehiclesTimer.schedule(newVehiclesTask, 3000);
					}
				}
			};
			redTimer.schedule(redTimerTask, 10000);
		} else if (type == 3) {
			special.max_vel = 0.1;
			special.pos[0] = 1000 * Simulation.pix * Math.random();
			special.pos[1] = 800 * Simulation.pix * Math.random();
			// Start timer to remove after 10 seconds
			redTimerTask = new TimerTask() {
				public void run() {
					synchronized (Simulation.this) {
						allVehicles.removeIf(v -> v.type == 3);
						if (specialVehicleType == 3) specialVehicleType = 0;
						repaint();
					}
				}
			};
			redTimer.schedule(redTimerTask, 10000);
		}
		double angle = 2 * Math.PI * Math.random();
		special.vel[0] = special.max_vel * Math.cos(angle);
		special.vel[1] = special.max_vel * Math.sin(angle);
		allVehicles.add(special);
		specialVehicleType = type;
	}

	public static void main(String args[]) {
		Simulation sim = new Simulation();
		GeneticAlgorithm ga = new GeneticAlgorithm();
		ga.run(sim);
		// Visualize only the final generation (orange)
		sim.resetVehicles();
		for (Individual ind : ga.getFinalGeneration()) {
			sim.createVehicleWithParams(ind, 3); // Orange
		}
		// Start the interactive simulation
		sim.run();
	}

	public void run() {
		Vehicle v;
		while (true) {
			// Remove all but one special vehicle if any bug
			allVehicles.removeIf(veh -> (veh.type == 1 || veh.type == 2 || veh.type == 3) && veh.type != specialVehicleType);
			// Contact logic for red special vehicle
			Vehicle special = null;
			for (Vehicle veh : allVehicles) {
				if (veh.type == 1 || veh.type == 2 || veh.type == 3) {
					special = veh;
					break;
				}
			}
			// Red special vehicle logic
			if (special != null && special.type == 1) {
				ArrayList<Vehicle> toRemove = new ArrayList<>();
				for (Vehicle veh : allVehicles) {
					if (veh.type == 0) {
						double dx = veh.pos[0] - special.pos[0];
						double dy = veh.pos[1] - special.pos[1];
						double dist = Math.sqrt(dx * dx + dy * dy);
						double contactThreshold = (veh.FZL + special.FZL) * 3.0;
						if (dist < contactThreshold) {
							toRemove.add(veh);
						}
					}
				}
				if (!toRemove.isEmpty()) {
					allVehicles.removeAll(toRemove);
				}
			}
			// Yellow special vehicle logic
			if (special != null && special.type == 2) {
				ArrayList<Vehicle> toRemove = new ArrayList<>();
				for (Vehicle veh : allVehicles) {
					if (veh.type == 0 && !veh.isNew && !veh.isOrange) {
						double dx = veh.pos[0] - special.pos[0];
						double dy = veh.pos[1] - special.pos[1];
						double dist = Math.sqrt(dx * dx + dy * dy);
						double contactThreshold = (veh.FZL + special.FZL) * 3.0;
						if (dist < contactThreshold) {
							toRemove.add(veh);
						}
					}
				}
				if (!toRemove.isEmpty()) {
					allVehicles.removeAll(toRemove);
				}
			}
			// Move all vehicles
			for (int i = 0; i < allVehicles.size(); i++) {
				v = allVehicles.get(i);
				v.move(allVehicles);
			}
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {}
			repaint();
		}
	}

	public ArrayList<Vehicle> getAllVehicles() {
		return allVehicles;
	}

	// Add this method to clear all vehicles (except special vehicles)
	public void resetVehicles() {
		allVehicles.removeIf(v -> v.type == 0);
	}

	// Add this method to create a single vehicle with parameters from an Individual
	public void createVehicleWithParams(Individual ind, int generation) {
		Vehicle car = new Vehicle(generation);
		car.setCohesionWeight(ind.cohesionWeight);
		car.setAlignmentWeight(ind.alignmentWeight);
		car.setSeparationWeight(ind.separationWeight);
		allVehicles.add(car);
		repaint();
	}
}
