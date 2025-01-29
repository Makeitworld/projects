# %%
import numpy as np
import matplotlib.pyplot as plt

# Constants
L = 6.8  # Dimensionless side length of the cubic cell
system_size=L
N = 256  # Number of atoms
epsilon = 1.0  # Depth of the potential well
sigma = 1.0    # Finite distance at which the potential is zero
kB = 1.38e-23  # Boltzmann constant in J/K
mass = 1.0     # Mass of each atom (in arbitrary units)
T_target = 300 # Target temperature in K

# Read initial positions from file
positions = np.loadtxt('liquid256.txt')

# Step (a): Randomly initialize velocities with zero total momentum
def initialize_velocities(N):
    """
    Initialize velocities uniformly between -1 and 1 for all x, y, z components,
    ensuring zero total momentum and scaling them to match the target temperature.
    """
    # Step 1: Generate uniform velocities in the range [-1, 1]
    velocities = np.random.uniform(-1, 1, (N, 3))  # Shape (N, 3)

    # Step 2: Adjust to ensure zero net momentum
    velocities -= np.mean(velocities, axis=0)      # Zero total momentum

    # Step 3: Scale to match target temperature
    # Compute the initial kinetic energy per atom
    initial_kinetic_energy = 0.5 * mass * np.sum(velocities**2) / N

    # Calculate the scaling factor
    target_kinetic_energy = 1.5 * kB * T_target  # 3/2 k_B T per atom
    scaling_factor = np.sqrt(target_kinetic_energy / initial_kinetic_energy)

    # Apply scaling factor
    velocities *= scaling_factor

    return velocities


velocities = initialize_velocities(N)

# %%
def apply_pbc(delta, box_size):
    """Apply periodic boundary conditions to delta."""
    delta -= box_size * np.round(delta / box_size)
    return delta

# %%
# Step (b): Calculate forces using Lennard-Jones potential with cutoff and continuous sche,e 
def lj_force(r):
    """Calculate Lennard-Jones force."""
    r7 = r**7
    r6 = r**6
    r13 = r7 * r6
    return (48 * epsilon / r13 - 24 * epsilon / r7)

def compute_forces(positions):
    """Compute forces on all atoms."""
    forces = np.zeros_like(positions)
    for i in range(N):
        for j in range(i + 1, N):
            # Periodic boundary conditions
            delta = positions[i] - positions[j]
            delta=apply_pbc(delta,system_size)
            r_cut=2.5 * sigma
            r2 = np.dot(delta, delta)
            if r2 < (r_cut)**2:   # Apply cutoff at r < 2.5*sigma
                r = np.sqrt(r2)
                f_ij = lj_force(r)-lj_force(r_cut)
                forces[i] += f_ij * delta / r  # Normalize direction
                forces[j] -= f_ij * delta / r

    return forces

# %%
def lj_potential(r):
    """Calculate Lennard-Jones potential."""
    return 4 * epsilon * ((sigma / r)**12 - (sigma / r)**6)

def compute_potential_energy(positions):
        """Compute total potential energy of the system."""
        potential_energy = 0.0
        for i in range(N):
            for j in range(i + 1, N):
                # Periodic boundary conditions
                delta = positions[i] - positions[j]
                delta=apply_pbc(delta,system_size) # Nearest image convention
                r2 = np.dot(delta, delta)
                r_cut=2.5 * sigma
                if r2 < (r_cut)**2:  # Apply cutoff at r < 2.5*sigma
                    r = np.sqrt(r2)
                    #smooth functiona s discussed in class
                    potential_energy += lj_potential(r)
                    potential_energy-=lj_potential(r_cut)
                    potential_energy-=(r-r_cut)*lj_force(r_cut)

        return potential_energy

# %%
# Step (c): Calculate instantaneous temperature from kinetic energy
def compute_temperature(velocities):
    kinetic_energy = 0.5 * mass * np.sum(velocities**2)
    temperature = (2/3) * (kinetic_energy / (N * kB))
    return temperature

# %%
def check_temperature_equilibration(temperatures, T_target, temp_diff_threshold=1.0, std_threshold=0.5):
    
    if len(temperatures) < 100:
        return False  # Not enough data points yet
    
    recent_temperatures = temperatures[-100:]
    mean_temp = np.mean(recent_temperatures)
    temp_std = np.std(recent_temperatures)
    
    # Check both mean deviation and fluctuation stability
    return (np.abs(mean_temp - T_target) < temp_diff_threshold and temp_std < std_threshold)


# %%
# Step (f): Time evolution using Velocity Verlet algorithm and plotting results.
time_steps = 200   # Total number of time steps
dt = 0.01          # Time step size in LJ units

kinetic_energies = []
potential_energies = []
total_energies = []
temperatures = []
momentum_x = []
momentum_y = []
momentum_z = []

positions_history = [positions.copy()]

for step in range(time_steps):
    forces = compute_forces(positions)
    
    # Update positions: x(t+dt) = x(t) + v(t)*dt + (f/m)*(dt^2)/2
    positions += velocities * dt + (forces / mass) * (dt**2) / 2
    momentum_x.append(np.sum(velocities[:,0]))
    momentum_y.append(np.sum(velocities[:,1]))
    momentum_z.append(np.sum(velocities[:,2]))
    # Update velocities: v(t+dt) = v(t) + (f/m)*(dt/2)
    new_forces = compute_forces(positions)
    velocities += ((forces + new_forces) / (2 * mass)) * dt
    
    
    # Store energies and temperature for plotting
    kinetic_energy = 0.5 * mass * np.sum(velocities**2)
    potential_energy = compute_potential_energy(positions)
    if(step<=200):
        kinetic_energies.append(kinetic_energy)
        potential_energies.append(potential_energy)
        total_energies.append(kinetic_energy + potential_energy)
        temperatures.append(compute_temperature(velocities))
    if(step>100):
        if check_temperature_equilibration(temperatures,300):
            print("System has equilibrated.")
    

# Plotting results:
plt.figure(figsize=(12,8))
plt.subplot(2,2,1)
plt.plot(kinetic_energies, label='Kinetic Energy')
plt.plot(potential_energies, label='Potential Energy')
plt.plot(total_energies, label='Total Energy')
plt.xlabel('Time Steps')
plt.ylabel('Energy')
plt.savefig('energy.png')
plt.legend()

plt.subplot(2,2,2)
plt.plot(temperatures, label='Temperature')
plt.savefig('temperatures.png')
plt.xlabel('Time Steps')
plt.ylabel('Temperature')
plt.savefig('temperatures.png')
plt.legend()
plt.show()

# %%
# Plot momentum conservation:
plt.figure(figsize=(10,5))
plt.plot(momentum_x, label='Momentum X')
plt.plot(momentum_y, label='Momentum Y')
plt.plot(momentum_z, label='Momentum Z')
plt.xlabel('Time Steps')
plt.ylabel('Momentum')
plt.legend()
plt.title('Conservation of Momentum over Time')
plt.savefig('momentum.png')
plt.show()


