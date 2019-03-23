# queue-simulator
This is the simulator to test the performance.
You can run it as follow:

```bash
./gradlew runScenario -Pscenario=Demo -Pexec=100 -Pcreate=300 -PoutPath=out/status.csv
```

You can pass following parameters.
* `scenario`: the name of the scenario class
* `exec`: the execution time of actions (ms)
* `create`: time required to create a new container (ms)
* `outPath`: the path where the simulation log will be written to
