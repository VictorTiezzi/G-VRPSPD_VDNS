Calibration AVRPSPD instances generated in the same file format as the uploaded benchmark.

Method summary:
- 50 customers plus one depot.
- Two class-6 geographic scenarios: SCA = uniformly distributed customers; CON = urban/clustered customers.
- Symmetric base costs are Euclidean distances rounded to 4 decimals.
- Asymmetry is introduced with lambda_ij in [1.0, 1.5] for depot-to-customer arcs and customer-customer arcs with i<j; lambda_ij = 1.0 otherwise.
- Directed Floyd-Warshall is applied to decrease costs violating the triangle inequality.
- Delivery/pick-up split follows r_i = min{x_i/y_i, y_i/x_i}; d_i = r_i * delta_i and p_i = delta_i - d_i, rounded to 2 decimals.
- Capacities are rounded to 2 decimals. Files ending in 3 enumerate K=4 vehicles; files ending in 8 enumerate K=9 vehicles, matching the class-6 pattern observed in the original benchmark files. Q is rounded upward to two decimals so that KQ is at least the larger of total delivery and total pick-up demand.

Files:
- CAL-SCA3-10.txt
- CAL-SCA8-10.txt
- CAL-CON3-10.txt
- CAL-CON8-10.txt
- summary.csv
