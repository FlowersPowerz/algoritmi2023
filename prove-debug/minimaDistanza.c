#include <stdio.h>
#include <stdlib.h>

int ROWS_M = 6, COLUMNS_N = 7;

void createPositionValuesMatrix(int positionValues[ROWS_M][COLUMNS_N]) {
	// Calcolo della massima distanza possibile da una cella sulla griglia
	int maxDistance = (ROWS_M + COLUMNS_N) / 2;

	// Ciclo attraverso ogni riga e colonna sulla griglia di gioco
	for (int row = 0; row < ROWS_M; row++) {
		for (int col = 0; col < COLUMNS_N; col++) {
			// Calcolo della distanza minima dalla riga corrente e dalla riga opposta
			int distanceRow = (row < ROWS_M - 1 - row) ? row : ROWS_M - 1 - row;
			// Calcolo della distanza minima dalla colonna corrente e dalla colonna opposta
			int distanceCol = (col < COLUMNS_N - 1 - col) ? col : COLUMNS_N - 1 - col;
			// Calcolo della distanza minima tra le due distanze calcolate in precedenza
			int distance = (distanceRow < distanceCol) ? distanceRow : distanceCol;
			// Assegnazione del valore della posizione corrente nella matrice di posizioni
			positionValues[row][col] = maxDistance - distance;
		}
	}
}

int main(int argc, char *argv[]) {
	if(argc == 3) {
		ROWS_M = atoi(argv[1]);
		COLUMNS_N = atoi(argv[2]);
	} else
		printf("i used default values, u can use this $ ./minimaDistanza.o <ROWS> <COLUMNS>\n\n");

	int positionValues[ROWS_M][COLUMNS_N];
	char asciiArtMatrix[ROWS_M][COLUMNS_N];
	
	// Filling the matrix with ASCII art characters representing distance values
	createPositionValuesMatrix(positionValues);
	
	// Filling the matrix with ASCII art characters
	for (int row = 0; row < ROWS_M; row++) {
		for (int col = 0; col < COLUMNS_N; col++) {
			// Assigning ASCII art characters based on distance values
			asciiArtMatrix[row][col] = '0' + positionValues[row][col];
		}
	}
	
	// Printing the ASCII art matrix
	for (int row = 0; row < ROWS_M; row++) {
		for (int col = 0; col < COLUMNS_N; col++) {
			printf("%c ", asciiArtMatrix[row][col]);
		}
		printf("\n");
	}
	
	return 0;
}

