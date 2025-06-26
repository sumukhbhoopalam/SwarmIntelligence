import java.util.*;

public class GeneticAlgorithm {
    public List<Individual> population;
    public int populationSize = 20;
    public double mutationRate = 0.1;
    public int generations = 50;
    private Individual bestIndividual = null;

    //Constructor that initializes the population with random individuals.
    public GeneticAlgorithm() {
        population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            population.add(new Individual());
        }
    }

    //Runs the genetic algorithm for three generations (black, green, orange), 
    //evolving and evaluating each. Selects the best individuals and updates the simulation accordingly.
    public void run(Simulation simulation) {
        // --- Generation 1: Black ---
        population = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            population.add(new Individual());
        }
        // Evaluate black generation
        Map<Individual, Double> fitnessMap = new HashMap<>();
        for (Individual ind : population) {
            synchronized (simulation) {
                simulation.resetVehicles();
                simulation.createVehicleWithParams(ind, 1); // 1 = black
                double fitness = calculateCohesionFitness(simulation.getAllVehicles());
                fitnessMap.put(ind, fitness);
            }
        }
        // Select best black parents
        List<Individual> bestParents = selectBestN(fitnessMap, 20);

        // --- Generation 2: Green ---
        population = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            Individual parent1 = bestParents.get((int)(Math.random() * bestParents.size()));
            Individual parent2 = bestParents.get((int)(Math.random() * bestParents.size()));
            Individual child = crossover(parent1, parent2);
            child.mutate(mutationRate);
            population.add(child);
        }
        // Evaluate green generation
        fitnessMap = new HashMap<>();
        for (Individual ind : population) {
            synchronized (simulation) {
                simulation.resetVehicles();
                simulation.createVehicleWithParams(ind, 2); // 2 = green
                double fitness = calculateCohesionFitness(simulation.getAllVehicles());
                fitnessMap.put(ind, fitness);
            }
        }
        // Select best green parents
        bestParents = selectBestN(fitnessMap, 20);

        // --- Generation 3: Orange ---
        population = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            Individual parent1 = bestParents.get((int)(Math.random() * bestParents.size()));
            Individual parent2 = bestParents.get((int)(Math.random() * bestParents.size()));
            Individual child = crossover(parent1, parent2);
            child.mutate(mutationRate);
            population.add(child);
        }
        // Evaluate orange generation
        fitnessMap = new HashMap<>();
        for (Individual ind : population) {
            synchronized (simulation) {
                simulation.resetVehicles();
                simulation.createVehicleWithParams(ind, 3); // 3 = orange
                double fitness = calculateCohesionFitness(simulation.getAllVehicles());
                fitnessMap.put(ind, fitness);
            }
        }
        // Store the best overall individual
        bestIndividual = selectBestN(fitnessMap, 1).get(0);
    }

    //Sets the cohesion, alignment, and separation weights of all vehicles in the simulation to 
    //those of the given individual. Used to apply genetic parameters to the simulation.
    private void applyParametersToSimulation(Simulation simulation, Individual ind) {
        // You need to implement this: set the weights in all vehicles
        for (Vehicle v : simulation.getAllVehicles()) {
            v.setCohesionWeight(ind.cohesionWeight);
            v.setAlignmentWeight(ind.alignmentWeight);
            v.setSeparationWeight(ind.separationWeight);
        }
    }

    //Calculates the fitness of an individual based on how close their vehicles are to each other.
    private double calculateCohesionFitness(List<Vehicle> vehicles) {
        int n = vehicles.size();
        double totalDistance = 0.0;
        int count = 0;
        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                double dx = vehicles.get(i).pos[0] - vehicles.get(j).pos[0];
                double dy = vehicles.get(i).pos[1] - vehicles.get(j).pos[1];
                totalDistance += Math.sqrt(dx * dx + dy * dy);
                count++;
            }
        }
        double averageDistance = (count > 0) ? totalDistance / count : 0.0;
        return 1.0 / (1.0 + averageDistance); // Higher fitness = more cohesion
    }

    //Selects a parent for crossover based on fitness. Uses a tournament selection method.
    private Individual selectParent(Map<Individual, Double> fitnessMap) {
        // Tournament selection
        List<Individual> individuals = new ArrayList<>(fitnessMap.keySet());
        Individual best = individuals.get((int)(Math.random() * individuals.size()));
        for (int i = 0; i < 2; i++) {
            Individual contender = individuals.get((int)(Math.random() * individuals.size()));
            if (fitnessMap.get(contender) > fitnessMap.get(best)) {
                best = contender;
            }
        }
        return best.copy();
    }
    //Performs a simple average crossover between two individuals to create a new child.
    //Implements simple genetic crossover.
    private Individual crossover(Individual p1, Individual p2) {
        // Simple average crossover
        return new Individual(
            (p1.cohesionWeight + p2.cohesionWeight) / 2.0,
            (p1.alignmentWeight + p2.alignmentWeight) / 2.0,
            (p1.separationWeight + p2.separationWeight) / 2.0
        );
    }

    //Returns the best individual found during the genetic algorithm run.
    public Individual getBestIndividual() {
        return bestIndividual;
    }

    // Helper to select the top N individuals by fitness
    private List<Individual> selectBestN(Map<Individual, Double> fitnessMap, int n) {
        List<Map.Entry<Individual, Double>> entries = new ArrayList<>(fitnessMap.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<Individual> best = new ArrayList<>();
        for (int i = 0; i < n && i < entries.size(); i++) {
            best.add(entries.get(i).getKey().copy());
        }
        return best;
    }

    // Add a getter for the final population (orange generation)
    public List<Individual> getFinalGeneration() {
        return population;
    }
} 