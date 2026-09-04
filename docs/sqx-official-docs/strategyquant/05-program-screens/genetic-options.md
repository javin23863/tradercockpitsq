# Settings – Genetic options

- Source: <https://strategyquant.com/doc/strategyquant/genetic-options/>
- Section: StrategyQuant X › Program screens
- Fetched: 2026-09-04

---

this setting tab is visible only if you use **Genetic evolution** imode n **What to build -> Build Mode.**

[![SQ genetic options settings](https://strategyquant.com/wp-content/uploads/2019/02/sq_genetic_options.png)](https://strategyquant.com/wp-content/uploads/2019/02/sq_genetic_options.png)

You can configure various properties of genetic evolution here:

## Genetic options

### Max # of Generations

number of generations for which the population will be evolved. Recommended value from 5 – 100. It usually doesn’t bring much improvement to use too many generations, it is better to just restart the evolution and start from the scratch.

### Population size

size of population on one island. Recommended value from 10 – 100 or even more.

Please note that if you use more than one island, your total population will be (number of islands) x (population size), so make sure you’ll not have some very extreme number.

### Crossover and Mutation probability

probability of basic genetic operations. You can experiment with these values, for example increasing mutation should generate more diverse strategies.

## Island options

### Islands

number of separate islands. Islands are a new concept in SQ X, they allow running genetic evolution separately in isolated islands, with occasional migration of individuals between islands.

There’s no problem having just one island, the recommended value is 1-10. It doesn’t make much sense to use more than 10 islands, it could make your total population very big and it would take a lot of time to just evolve one generation.

### Migrate every Xth generation

how often to migrate some individuals from island to island. It is usually good to migrate the individuals, it can “unlock’ some island that got stuck in the local minimum. It shouldn’t be too often, because then we’d lose the diversity of the independent islands.  
Recommended value could be every 10 generations.

### Population migration rate

how many strategies in the population will be migrated. It should be something like 1-5 strategies, depending on your population size, so for population size=10 use value like 10-20%, for population size=100 use value like 1-5%.

## Initial population generation

Genetic evolution starts from some initial generation. It will either be generated randomly, or you could use some existing strategies as initial population and try to improve upon them.

### Use strategies from Initial population databank as evolution start

checking this will show you one more databank “Initial population’ where you can load your existing strategies. These strategies will then be used as initial population. If there is not enough of them, the rest will be generated randomly.

Please note that initial population from databank is NOT filtered using Initial population filter.

### Generated decimation coefficient

Decimation means that there will be X-times more strategies (that pass filters) generated than required, and from these the best will be chosen.

If you set decimation for example to 3, it will generate 3x more strategies for initial population and choose the best from this.

Using decimation will improve quality of initial population, but it will take much more time to generate it.

Please use it wisely, because it can greatly increase the number of strategies that need to be generated for initial population, so ii might take very long time to just generate the initial strategies before evolution even starts!

## Filter generated initial population

Here you can set up filter to set some basic minimum that a strategy should have to be in the initial generation.

Please use it wisely, genetic evolution should be able to improve any population of strategies, so don’t be too strict with your initial generation.  
The only recommended filter to use is by number of trades – to filter out strategies that are not trading at all.

## Evolution management

### Start again when finished

if checked it will restart the building process when it finished. So you can let it run autonomously, and SQ will evolve more and more populations until you stop it.

### Restart evolution fitness if..

it will restart evolution if we are stagnating in fitness – this means that the population as a whole is not improving, so it is better to start again from the beginning.

## “Fresh blood”

### Detect same strategies and replace them with new ones

It will detect the same strategies every generation, and randomy generate new ones in their place. This could help making strategies more diversified.

### Replace X % of the weakest startegies with newly generated ones

similar option, it can replace the weakest (worst) strategies with newly generated ones.

### Show last generation databank

displays databank that contains the current generation – only for the first island.
