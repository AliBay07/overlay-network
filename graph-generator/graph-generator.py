import sys

try:
    import networkx
    import matplotlib
    import numpy
except ImportError:
    print("Required packages not found. Installing...")
    try:
        import subprocess
        subprocess.run(["pip", "install", "networkx", "matplotlib", "numpy"], check=True)
    except subprocess.CalledProcessError:
        print("Failed to install required packages.")
        sys.exit(1)

import networkx as nx
import matplotlib.pyplot as plt

def create_graph_from_matrix(matrix):
    num_nodes = len(matrix)
    G = nx.Graph()

    G.add_nodes_from(range(num_nodes))

    for i in range(num_nodes):
        for j in range(num_nodes):
            if matrix[i][j] == '1':
                G.add_edge(i, j)

    return G

def main():
    if len(sys.argv) != 2:
        sys.exit(1)

    input_file = f"{sys.argv[1]}"
    output_file = "graphs/graph.png"

    with open(input_file, 'r') as file:
        matrix = [line.strip() for line in file]

    G_physical = create_graph_from_matrix(matrix)

    pos_physical = nx.spring_layout(G_physical)
    nx.draw(G_physical, pos_physical, with_labels=True, node_size=700, node_color='skyblue', font_size=10, font_weight='bold')
    plt.savefig(output_file, format="PNG")

if __name__ == "__main__":
    main()
