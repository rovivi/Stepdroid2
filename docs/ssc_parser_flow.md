# SSC Parser Flow

```plantuml
@startuml
class FileSSC
class NoteLayoutCalculator
class StepsDrawerGL
class GameRow
class Note
class NoteType
FileSSC --> GameRow
GameRow --> Note
Note --> NoteType
FileSSC --> NoteLayoutCalculator
StepsDrawerGL --> NoteLayoutCalculator
StepsDrawerGL --> Note
@enduml
```
