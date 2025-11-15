# Scastie Laminar Migration

This directory contains the new Laminar-based UI implementation for Scastie.

## Structure

```
laminar/
├── ScastieStore.scala       # Main reactive state store (replaces ScastieBackend)
├── LaminarApp.scala         # Application entry point and root component
├── components/              # Laminar UI components
│   └── RunButton.scala      # Example migrated component
└── README.md               # This file
```

## Migration Status

### Phase 1: Foundation ✅
- [x] Scala 3.3.6 setup
- [x] Laminar 17.1.0 dependency
- [x] Waypoint 8.0.0 router dependency
- [x] Reactive state store (ScastieStore)
- [x] Application scaffold (LaminarApp)
- [x] Proof-of-concept component (RunButton)

### Phase 2: Component Migration (In Progress)
- [ ] Simple components (buttons, status indicators)
- [ ] Container components (Console, TopBar, etc.)
- [ ] Complex components (SideBar, BuildSettings)
- [ ] Editor integration (CodeMirror)
- [ ] Modals
- [ ] Root components (MainPanel, Scastie)

### Phase 3: Router & Navigation (Pending)
- [ ] Waypoint router setup
- [ ] Route definitions
- [ ] Navigation integration

## Key Concepts

### State Management

**React (Old):**
```scala
class ScastieBackend(scope: BackendScope[_, ScastieState]) {
  def toggleTheme: Callback =
    scope.modState(s => s.copy(isDarkTheme = !s.isDarkTheme))
}
```

**Laminar (New):**
```scala
class ScastieStore {
  private val stateVar = Var(initialState)
  val isDarkThemeSignal: Signal[Boolean] = stateVar.signal.map(_.isDarkTheme)

  def toggleTheme(): Unit =
    stateVar.update(s => s.copy(isDarkTheme = !s.isDarkTheme))

  val toggleThemeObserver: Observer[Unit] =
    Observer[Unit](_ => toggleTheme())
}
```

### Component Pattern

**React (Old):**
```scala
object MyComponent {
  case class Props(value: String, onChange: Callback)

  private val component = ScalaComponent
    .builder[Props]("MyComponent")
    .render_P { props => div(...) }
    .build

  def apply(props: Props) = component(props)
}
```

**Laminar (New):**
```scala
object MyComponent {
  def apply(
    value: Signal[String],
    onChange: Observer[String]
  ): HtmlElement =
    div(
      input(
        controlled(
          value <-- value,
          onInput.mapToValue --> onChange
        )
      )
    )
}
```

### Key Differences

1. **No Virtual DOM**: Laminar updates the real DOM directly using FRP
2. **Signals & Observers**: Replace state + callbacks
3. **No Lifecycle Methods**: Effects are managed via FRP operators
4. **Simpler API**: No builders, no mounting logic - just functions returning elements

## Running the Migration

Currently both React and Laminar code coexist. To test:

1. Build: `sbt client/fastLinkJS`
2. The Laminar app is at `org.scastie.client.laminar.LaminarApp`
3. React app continues to work at `org.scastie.client.ClientMain`

## Next Steps

1. Migrate simple leaf components (buttons, indicators)
2. Test each component in isolation
3. Gradually replace React components with Laminar equivalents
4. Remove React dependencies when migration is complete

## Resources

- [Laminar Documentation](https://laminar.dev/)
- [Airstream Documentation](https://github.com/raquo/Airstream)
- [Waypoint Router](https://github.com/raquo/Waypoint)
- [Laminar Examples](https://demo.laminar.dev/)
