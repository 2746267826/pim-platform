# System Overview

分层总览（节点为 layer，非全量文件）。全量关系见 [interactive/index.html](interactive/index.html)。

```mermaid
flowchart TB
  subgraph clients [Clients]
    WEB[client-web]
    WIN[client-windows]
    AND[client-android]
  end
  subgraph api_layer [API]
    API[Pim.Api]
  end
  subgraph modules [Modules]
    CAL[module.calendar]
    PC[module.pctracker]
    MOB[module.mobile]
    FIL[module.files]
    QN[module.quicknotes]
    ST[module.stats]
  end
  subgraph platform [Platform]
    CORE[core]
    INFRA[infrastructure]
  end
  TESTS[tests]
  WEB --> API
  WIN --> API
  AND --> API
  API --> modules
  API --> CORE
  API --> INFRA
  modules --> CORE
  modules --> INFRA
  INFRA --> CORE
  TESTS -.-> WEB
  TESTS -.-> API
  TESTS -.-> modules
  TESTS -.-> WIN
  TESTS -.-> AND
```

## Layer docs

- [core](layers/core.md)
- [infrastructure](layers/infrastructure.md)
- [api](layers/api.md)
- [module-stats](layers/module-stats.md)
- [module-quicknotes](layers/module-quicknotes.md)
- [module-files](layers/module-files.md)
- [module-mobile](layers/module-mobile.md)
- [module-pctracker](layers/module-pctracker.md)
- [module-calendar](layers/module-calendar.md)
- [client-web](layers/client-web.md)
- [client-windows](layers/client-windows.md)
- [client-android](layers/client-android.md)
- [tests](layers/tests.md)