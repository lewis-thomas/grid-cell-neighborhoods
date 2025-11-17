import random
import argparse
import json
import sys

def generate_random_grid(rows, cols, positive_percentage):
    """
    Generates a 2D grid of random numbers using a memory-efficient algorithm.
    """
    total_cells = rows * cols
    num_positive = int(total_cells * (positive_percentage / 100.0))
    
    # --- Start with a grid of all negative numbers ---
    # This is the most memory-intensive part, but it's necessary for the final output.
    grid = [[-1 for _ in range(cols)] for _ in range(rows)]
    
    # --- Use a set to efficiently track which cells have been filled ---
    chosen_indices = set()
    
    print(f"Placing {num_positive} positive numbers in a {total_cells} cell grid...")
    
    # --- Loop until we have placed all the required positive numbers ---
    while len(chosen_indices) < num_positive:
        # Pick a random cell index from 0 to (rows*cols - 1)
        random_index = random.randint(0, total_cells - 1)
        
        # If we haven't used this index yet, place a positive number there
        if random_index not in chosen_indices:
            chosen_indices.add(random_index)
            
            # Convert the 1D index back into 2D row and column coordinates
            row = random_index // cols
            col = random_index % cols
            
            # Place a random positive number in the grid
            grid[row][col] = random.randint(1, 10)
            
    return grid

def format_custom_json(data):
    """
    Creates a JSON string with a specific format:
    - Outer structure is indented.
    - Each inner array (row) is compact on a single line.
    """
    lines = ['{\n  "data": [']
    grid = data["data"]
    
    for i, row in enumerate(grid):
        compact_row = json.dumps(row, separators=(',', ' '))
        lines.append(f'    {compact_row}')
        
        if i < len(grid) - 1:
            lines.append(',')
    
    lines.append('\n  ]\n}')
    return "".join(lines)

def main():
    """Main function to parse arguments and run the script."""
    parser = argparse.ArgumentParser(
        description="Generate a large JSON grid efficiently and save it to output.json."
    )
    
    parser.add_argument("rows", type=int, help="The number of rows in the grid.")
    parser.add_argument("cols", type=int, help="The number of columns in the grid.")
    parser.add_argument("percentage", type=float, help="The percentage of positive numbers (0-100).")

    args = parser.parse_args()

    if args.rows <= 0 or args.cols <= 0:
        print("Error: Grid dimensions must be positive integers.", file=sys.stderr)
        sys.exit(1)

    if not (0 <= args.percentage <= 100):
        print("Error: Percentage must be between 0 and 100.", file=sys.stderr)
        sys.exit(1)

    # --- Generate and Write the Grid to a File ---
    my_grid = generate_random_grid(args.rows, args.cols, args.percentage)
    
    output_json = {
        "data": my_grid
    }
    
    output_filename = "output.json"
    
    formatted_json_string = format_custom_json(output_json)
    
    with open(output_filename, "w") as f:
        f.write(formatted_json_string)
        
    print(f"Successfully generated grid and saved to {output_filename}")


if __name__ == "__main__":
    main()

