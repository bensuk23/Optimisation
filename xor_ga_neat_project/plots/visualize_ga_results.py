# Fichier: plots/visualize_ga_neat_results.py (Version unifiée GA/NEAT)

import pandas as pd
import matplotlib.pyplot as plt
import os
import sys

# --- 1. Définition et Résolution du Chemin ---

def get_solver_choice():
    """Détermine le solveur à visualiser (GA ou NEAT) via argument ou prompt."""
    if len(sys.argv) > 1 and sys.argv[1].lower() in ['ga', 'neat']:
        return sys.argv[1].lower()

    # Demander à l'utilisateur si aucun argument n'est fourni ou s'il est invalide
    print("Veuillez choisir le solveur à visualiser :")
    print("1. GA Classique (tapez 'ga')")
    print("2. NEAT Simplifié (tapez 'neat')")
    choice = input().lower()
    while choice not in ['ga', 'neat']:
        print("Choix invalide. Veuillez taper 'ga' ou 'neat' :")
        choice = input().lower()
    return choice

def setup_paths(solver_type):
    """Configure les chemins d'accès et de sortie en fonction du solveur."""
    current_dir = os.path.dirname(__file__)

    if solver_type == 'ga':
        # Chemin pour le log GA: ../../ga_progression_log.csv
        relative_path = os.path.join(current_dir, '..', '..', 'ga_progression_log.csv')
        plot_name = 'ga_fitness_progression_finale.png'
        title_suffix = 'GA Classique'
    else: # neat
        # Chemin pour le log NEAT: ../../data_and_results/neat_progression_log.csv
        # Note: 'data_and_results/' est spécifié dans NeatXorSolver.java
        relative_path = os.path.join(current_dir, '..', '..', 'neat_progression_log_complet.csv')
        plot_name = 'neat_fitness_progression_finale.png'
        title_suffix = 'NEAT Simplifié'

    csv_file_path = os.path.abspath(relative_path)
    plot_output_path = os.path.join(current_dir, plot_name)

    return csv_file_path, plot_output_path, title_suffix

def load_data(csv_file_path):
    """Charge le fichier CSV, corrige les erreurs d'index et crée une colonne 'Generation' propre."""
    try:
        # Lire les données
        df = pd.read_csv(
            csv_file_path,
            header=0,
            names=['Generation_Corrupted', 'MaxFitness', 'AvgFitness']
        )

        df = df.dropna()
        df = df.reset_index(drop=True)
        df['Generation'] = df.index + 1

        # S'assurer que les colonnes de fitness sont des nombres
        df['MaxFitness'] = pd.to_numeric(df['MaxFitness'], errors='coerce')
        df['AvgFitness'] = pd.to_numeric(df['AvgFitness'], errors='coerce')

        # Gérer les NaN après conversion
        df = df.dropna()

        if df.empty:
            print(f"❌ Erreur: Le DataFrame est vide après nettoyage. Vos données sont soit illisibles, soit le CSV est corrompu.")
            sys.exit(1)

        return df
    except FileNotFoundError:
        print(f"❌ Erreur: Le fichier CSV n'a pas été trouvé à l'emplacement : {csv_file_path}")
        print("Conseil: Assurez-vous d'avoir exécuté le solveur Java correspondant (Main.java pour GA, NeatXorSolver.java pour NEAT) pour générer le fichier log.")
        sys.exit(1)
    except Exception as e:
        print(f"❌ Erreur lors du chargement des données: {e}")
        sys.exit(1)


def plot_results(df, plot_output_path, title_suffix):
    """Génère un graphique clair et professionnel de progression de la fitness."""

    plt.style.use('seaborn-v0_8-whitegrid')
    fig, ax = plt.subplots(figsize=(10, 6))

    # 1. Tracé des données
    ax.plot(df['Generation'], df['MaxFitness'],
            label='Max Fitness (Meilleur Individu)',
            color='red', linewidth=2, alpha=0.9)

    ax.plot(df['Generation'], df['AvgFitness'],
            label='Avg Fitness (Moyenne de la Population)',
            color='blue', linestyle='--', linewidth=1.5, alpha=0.7)

    # 2. Améliorations de l'affichage
    ax.set_title(f'Progression de la Fitness pour le Problème XOR ({title_suffix})', fontsize=16, fontweight='bold')
    ax.set_xlabel('Génération', fontsize=12)
    ax.set_ylabel('Fitness (Échelle Logarithmique)', fontsize=12)

    # Limitation de l'axe Y pour se concentrer sur la zone de convergence.
    ax.set_yscale('log')
    ax.legend(loc='lower right', fontsize=10)

    plt.tight_layout()
    plt.savefig(plot_output_path)
    print(f"\n✅ Graphique professionnel sauvegardé: {plot_output_path}")
    plt.show()

if __name__ == '__main__':
    solver_type = get_solver_choice()
    print(f"--- Démarrage de la Visualisation des Résultats pour {solver_type.upper()} (Python) ---")

    csv_path, plot_path, title = setup_paths(solver_type)
    data_frame = load_data(csv_path)

    print("\nAperçu des données CSV chargées (5 dernières lignes - Axe X Corrigé) :")
    print(data_frame[['Generation', 'MaxFitness', 'AvgFitness']].tail())

    plot_results(data_frame, plot_path, title)