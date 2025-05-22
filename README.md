# Swarm Intelligence Simulation

This project implements a swarm intelligence simulation that demonstrates emergent behavior through simple individual rules. The simulation features autonomous agents (vehicles) that interact with each other and respond to different types of events in their environment.

## Overview

The simulation creates a dynamic environment where multiple agents interact based on three fundamental rules:
- **Cohesion**: Agents try to move towards the center of mass of nearby agents
- **Separation**: Agents maintain a minimum distance from each other
- **Alignment**: Agents try to match their velocity with nearby agents

## Event Types

The simulation includes three types of events that affect the swarm behavior:

### 1. Red Event (Danger)
- **Behavior**: The entire swarm must flee from the red event
- **Consequences**: 
  - If an individual comes in contact with the red event, it disappears
  - New individuals are generated to replace the lost ones
  - The swarm maintains its size while avoiding the danger

### 2. Yellow Event (Target)
- **Behavior**: The swarm must surround and contain the yellow event
- **Consequences**:
  - If individuals fail to make contact with the yellow event, it disappears
  - New individuals are generated to maintain the swarm
  - The swarm must coordinate to maintain contact with the target

### 3. Orange Event (Split Decision) - Optional
- **Behavior**: The swarm splits into two groups
  - Half of the swarm must flee from the orange event
  - The other half must move towards and interact with the orange event
- **Consequences**:
  - Creates a dynamic decision-making scenario
  - Tests the swarm's ability to split and coordinate different behaviors

## Technical Details

### Parameters
- Number of vehicles: 160
- Maximum velocity: 1.0 units
- Maximum acceleration: 0.05 units
- Separation radius: 7 units
- Cohesion radius: 25 units
- Behavior weights:
  - Cohesion: 0.05
  - Separation: 0.55
  - Alignment: 0.4

### Requirements
- Java Runtime Environment (JRE)
- Java Swing for visualization

### Running the Simulation
1. Compile the Java files
2. Run the Simulation class
3. The simulation window will open with the swarm in motion
4. Events will appear randomly in the simulation space

## Future Improvements
- Add configuration options for event parameters
- Implement more complex event behaviors
- Add data logging and analysis capabilities
- Improve visualization options
- Add user interaction controls
- Implement 3D visualization

## Contributing
Feel free to contribute to this project by:
- Adding new event types
- Improving the visualization
- Optimizing the swarm behavior
- Adding new features
- Reporting bugs

## License
This project is open source and available for educational and research purposes. 