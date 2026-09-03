# Interface Sink Divergence Companion

Flags a call through an interface-typed reference, passing tainted
HTTP input, where real implementations diverge on whether they log it
unsanitized.

## Why it exists

CWE-117/CWE-532: a caller tested against ONE implementation (which
never logs sensitive input) silently gets a different implementation
in production that does -- the interface type alone never guarantees
which behavior you get once dependency injection swaps implementations.
No dedicated Marketplace plugin found for this exact angle.

## Why built this way

- **Combines two techniques this catalog already proved separately**
  -- real Class Hierarchy Analysis (`interface-exception-divergence-
  companion`'s own `ClassInheritorsSearch`-based resolution of EVERY
  real implementation) with taint-to-sink detection
  (`log-injection-companion`'s own logging-call recognition), run
  INSIDE each implementation's own body, not just at the call site.
- **The interface method's parameter POSITION is the correlation key**
  -- the same argument index at the call site is checked against the
  corresponding parameter in each implementation's own override,
  since parameter names can differ between implementations.

## v0.1 scope — stated honestly, not exhaustively

- Only interfaces with 2-10 real implementations in the project.
- Taint only through a bare reference or one-hop concatenation, both
  at the call site AND inside each implementation -- any wrapping call
  anywhere in the chain breaks it (treated as sanitized).
- Only `Logger.info/warn/error/debug/trace`-shaped sinks (same
  recognition as `log-injection-companion`).

## Usage

Open a Java file with an interface with 2+ implementations that
diverge on logging a parameter, called through the interface type with
tainted input -- the call site shows a warning.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us at
**gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
