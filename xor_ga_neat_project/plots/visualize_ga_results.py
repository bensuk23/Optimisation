import pandas as pd
import matplotlib.pyplot as plt
import os
import sys

def get_solver_choice():
    if len(sys.argv) > 1 and sys.argv[1].lower() in ['ga', 'neat']:
        return sys.argv[1].lower()

    print("Choisir le solveur :")
    print("1. GA ")
    print("2. NEAT ")
    choice = input().lower()
    while choice not in ['1', '2']:
        print("Choix invalide. Tapez '1' ou '2' :")
        choice = input().lower()
    return choice

def setup_paths(solver_type):
    current_dir = os.path.dirname(__file__)

    if solver_type == '1':
        relative_path = os.path.join(current_dir, '..', '..', 'ga_progression_log.csv')
        plot_name = 'ga_fitness_progression_finale.png'
        title_suffix = 'GA'
    else:
        relative_path = os.path.join(current_dir, '..', '..', 'neat_progression_log_complet.csv')
        plot_name = 'neat_fitness_progression_finale.png'
        title_suffix = 'NEAT'

    csv_file_path = os.path.abspath(relative_path)
    plot_output_path = os.path.join(current_dir, plot_name)

    return csv_file_path, plot_output_path, title_suffix

def load_data(csv_file_path):
    try:
        df = pd.read_csv(
            csv_file_path,
            header=0,
            names=['Generation_Corrupted', 'MaxFitness', 'AvgFitness']
        )

        df = df.dropna()
        df = df.reset_index(drop=True)
        df['Generation'] = df.index + 1

        df['MaxFitness'] = pd.to_numeric(df['MaxFitness'], errors='coerce')
        df['AvgFitness'] = pd.to_numeric(df['AvgFitness'], errors='coerce')

        df = df.dropna()

        if df.empty:
            print(f"Erreur: DataFrame vide.")
            sys.exit(1)

        return df
    except FileNotFoundError:
        print(f"Erreur: Fichier non trouvé: {csv_file_path}")
        sys.exit(1)
    except Exception as e:
        print(f"Erreur: {e}")
        sys.exit(1)

def plot_results(df, plot_output_path, title_suffix):
    plt.style.use('seaborn-v0_8-whitegrid')
    fig, ax = plt.subplots(figsize=(10, 6))

    ax.plot(df['Generation'], df['MaxFitness'],
            label='Max Fitness',
            color='red', linewidth=2, alpha=0.9)

    ax.plot(df['Generation'], df['AvgFitness'],
            label='Avg Fitness',
            color='blue', linestyle='--', linewidth=1.5, alpha=0.7)

    ax.set_title(f'Progression Fitness XOR ({title_suffix})', fontsize=16, fontweight='bold')
    ax.set_xlabel('Génération', fontsize=12)
    ax.set_ylabel('Fitness (Log)', fontsize=12)

    ax.set_yscale('log')
    ax.legend(loc='lower right', fontsize=10)

    plt.tight_layout()
    plt.savefig(plot_output_path)
    print(f"\nGraphique sauvegardé: {plot_output_path}")
    plt.show()

if __name__ == '__main__':
    solver_type = get_solver_choice()
    print(f"Visualisation : {solver_type.upper()}")

    csv_path, plot_path, title = setup_paths(solver_type)
    data_frame = load_data(csv_path)

    print("\nAperçu données :")
    print(data_frame[['Generation', 'MaxFitness', 'AvgFitness']].tail())

    plot_results(data_frame, plot_path, title)