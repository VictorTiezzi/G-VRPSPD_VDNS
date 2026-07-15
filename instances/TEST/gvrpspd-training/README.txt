GVRPSPD calibration instances generated from the supplied CMT benchmark format.

Methodology used:
- CMT11 was used as the structural template for 120-customer instances; CMT5 was used for 199-customer instances.
- Coordinates were mildly and deterministically perturbed while preserving the original CMT spatial pattern and depot.
- For each customer, total demand t was obtained from the original CMT instance as delivery + pickup and perturbed by a deterministic factor in [0.90, 1.10].
- The split between delivery and pickup follows the Salhi and Nagy / Olgun et al. rule based on r_i = min(x_i/y_i, y_i/x_i).
- To match the supplied CMT files, variant X uses delivery=(1-r_i)t_i and pickup=r_i t_i; variant Y swaps delivery and pickup.
- Capacity and TSPLIB-like .vrpspd structure were preserved.
- summary.csv reports total delivery/pickup and the minimum number of vehicles required by aggregate capacity.
