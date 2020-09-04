# Cats Bot

A Starcraft Broodwar Bot built in Scala. 

## Setup

### Linux

1. Follow the [JBWAPI Tutorial](https://github.com/JavaBWAPI/Java-BWAPI-Tutorial/wiki) to setup your Starcraft environment.
2. Open the project in Intellj and run it. It will crash but copy the classpath. Replaced `:` with `;` because on wine (Windows),
    it uses semicolons.
3. Paste the classpath in [gogo.sh](./gogo.sh) and [gogo-debug.sh](./gogo-debug.sh).
4. Run [gogo.sh](./gogo.sh)
## Technologies

- [Cats](https://typelevel.org/cats/)
- [Monix](https://monix.io/)