# queue-simulator
This is the simulator to test the performance.
You can run it as follow:

```bash
./gradlew runSimulator -PinMsg=50 -Pexec=100 -Pcreate=300
```

You can pass following parameters.
* `inMsg`: the number of messages per second
* `exec`: the execution time of actions (ms)
* `create`: time required to create a new container (ms)
