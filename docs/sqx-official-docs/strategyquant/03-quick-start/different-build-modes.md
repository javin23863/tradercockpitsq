# Different build modes

- Source: <https://strategyquant.com/doc/strategyquant/different-build-modes/>
- Section: StrategyQuant X › Quick start
- Fetched: 2026-09-04

---

Please check also the article [How does StrategyQuant work](../01-introduction/how-does-strategyquant-work.md) to understand how it generates the strategies.

There are two build mode types to choose from:

## Random Generation

In this mode StrategyQuant continually generates and tests new random strategies, one after another, until it is stopped.  
The top candidates (based on predefined criteria) are stored into Databank so you can review them later.

#### **Pros**:

- faster and simpler than genetic evolution
- it can run until it is stopped, so if you let it run for a few days it can generate and evaluate millions of strategies
- less prone to overfitting, strategi is not further optimalized or improved

#### **Cons**:

- once the strategy is generated it is not further evolved or optimized – but you can always use it in an initial population for the next build based on genetic evolution

## Genetic Evolution

StrategyQuant first generates initial population of random candidates (using the Random Generation mode) and then uses genetic evolution process to evolve the population and produce better and better candidates with each generation.

The process ends when predefined number of generations is reached or when there’s no further improvement.

#### **Pros**:

- in theory it should lead to strategies better than the initial random generation
- this means that the already good strategies in the first generation can be further improved
- search for profitable strategy in the trillions of possible combinations can be more effective with the power of genetic evolution

#### **Cons**:

- evolution can be slower
- sometimes the evolution can lead to the dead end, so the generation should be watched
- the group of generated strategies is limited by population size
- more prone to overfitting, it is basically an optimizaiton process

## Custom projects

Custom projects are not exactly another build mode type, but it is good to mention them here because they allow you to create workflows of multipele tasks – for example, you can run multiple random builds one after another, or use random generation, then folowed by genetic evolution that will use the strategies from random generation as its initial population.

Check [Introduction to custom projects](../09-custom-projects-and-tasks/introduction-to-custom-projects.md) for more information.
