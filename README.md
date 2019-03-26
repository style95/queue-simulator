# queue-simulator
This is the simulator to test the performance.
You can run it as follow:

```bash
./gradlew runScenario -Pscenario=Demo -PoutDir=out/demo -Pexec=100 -Pcreate=300
```

You can pass following parameters.
* `scenario`: the name of the scenario class
* `outDir`: the path where the simulation log will be written to
* `exec`: the execution time of actions (ms)
* `create`: time required to create a new container (ms)
