# Native StrategyQuant X plugins packaged with TraderCockpit

These are owner-supplied StrategyQuant X extensions. They run **inside** SQX
(Results plugins, authoring skills). TraderCockpit does not execute their Vue/HTML,
does not import their Python engines, and does not replace their numbers.

Install Results plugins into the authorized runtime:

`{SQX_HOME}/user/extend/ResultsPlugins/<plugin folder>/`

Authoring skills (SQX Lab, Custom Block) stay packaged here for native-block
authoring. They are not a second product UI and must not be imported as a
runtime quantitative engine.
