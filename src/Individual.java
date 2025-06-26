public class Individual {
    public double cohesionWeight;
    public double alignmentWeight;
    public double separationWeight;
    public static final double MIN_WEIGHT = 0.0;
    public static final double MAX_WEIGHT = 2.0;

    //The Individual.java file defines a data structure representing a single candidate solution 
    //(individual) for the genetic algorithm.
    //It encapsulates the weights for cohesion, alignment, and separation behaviors, 
    //and provides methods for initialization, mutation, and cloning.
    public Individual() {
        // Random initialization
        this.cohesionWeight = randomWeight();
        this.alignmentWeight = randomWeight();
        this.separationWeight = randomWeight();
    }

    public Individual(double cohesion, double alignment, double separation) {
        this.cohesionWeight = cohesion;
        this.alignmentWeight = alignment;
        this.separationWeight = separation;
    }

    private double randomWeight() {
        return MIN_WEIGHT + Math.random() * (MAX_WEIGHT - MIN_WEIGHT);
    }
    //Returns a new Individual with the same weights as this one. Used for cloning individuals 
    //during selection and elitism.
    public Individual copy() {
        return new Individual(cohesionWeight, alignmentWeight, separationWeight);
    }

    //Applies random mutation to the weights of an individual. 
    //Used to introduce variation in the population during evolution.
    public void mutate(double mutationRate) {
        boolean mutated = false;
        if (Math.random() < mutationRate) {
            cohesionWeight = randomWeight();
            System.out.printf("Mutation: New cohesionWeight = %.4f\n", cohesionWeight);
            mutated = true;
        }
        if (Math.random() < mutationRate) {
            alignmentWeight = randomWeight();
            System.out.printf("Mutation: New alignmentWeight = %.4f\n", alignmentWeight);
            mutated = true;
        }
        if (Math.random() < mutationRate) {
            separationWeight = randomWeight();
            System.out.printf("Mutation: New separationWeight = %.4f\n", separationWeight);
            mutated = true;
        }
        if (!mutated) {
            System.out.println("Mutation: No mutation occurred for this individual.");
        }
    }
} 