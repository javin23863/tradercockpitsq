# Sequential optimization

- Source: <https://strategyquant.com/doc/strategyquant/sequential-optimization/>
- Section: StrategyQuant X › Optimization
- Fetched: 2026-09-04

---

# Sequential optimization

Sequential optimization is a new optimization method available as both new optimization type and a new crosscheck in StrategyQuant X 132.

## How it works

Let’s say we have a strategy with 5 parameters: Param1, Param2, Param3, Param4, Param5.

Standard optimization will use brute force or genetic optimization approach to go through all the possible combinations of values for these five parameters and uses the variant with the best fitness.

Problems of standard optimization

1. **Easily leads to over-fitting**  
   The best variant is chosen by fitness, but this could be only an outlier value that fits the provided data, not a stable region. It is also impossible to define what a stable region is if you optimize by more than 2 parameters at once.
2. **It can be slow or does not cover all parameter space** if many parameters are optimized  
   If each of your five parameters has 100 possible values, then there is 100 x 100 x 100 x 100 x 100 possible combinations.

### How sequential optimization works

Sequentially. It optimizes these five parameters in five steps one by one.

So the sequential optimization process works as follows:

**Step 1** – the first parameter Param1 is optimized.  
The rest of parameters are left at their original values.  
The result is the optimal (most stable) value of Param1

**Step 2** – the second parameter Param2 is optimized.  
It uses the optimal value for Param1 (computed in the previous step, the rest of parameters are left at their original values.  
The result is the optimal (most stable) value of Param2.

**Step 3** …  
**Step 4** …

**Step5** – the fifth parameter Param5 is optimized.  
It uses the optimal value for Param1, Param2, Param3, Param4 computed in the previous steps.  
The result is the optimal (most stable) value of Param5, completing the optimization of all five parameters.

### How the stable value of a parameter is chosen

Another major difference from standard optimization is how the optimization process choses the optimal parameter value in every step.

Instead of choosing the variant with the best fitness, it chooses the variant that is in the middle of the best stable area as defined in configuration.

Stable area is the area where the defined number of results are not worse than some % of the best fitness of that region.

[![](https://strategyquant.com/wp-content/uploads/2021/06/stable_area-300x171.png)](https://strategyquant.com/wp-content/uploads/2021/06/stable_area-300x171.png)

This way it chooses the stable value, not the best one.

### What if the stable area is not found?

It can happen that no stable area is found for the specific parameter. In that case the original value is used.

This is also something that is used in the new cross check – if no stable area is found for a given percentage of the parameters then the strategy is refused.

### Note – Dependency on original parameters

Note that the optimization process optimizes only one parameter at a time – for example on the Step 1 only Param1 is optimized and the rest of parameters use original values.

This means that the result of this type of optimizations depends also on the original values.

It is possible that the result of the optimization in every step (and the resulting stable area and chosen value) would be different if different original values would be used on start.
