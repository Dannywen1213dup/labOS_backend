# Docker 新手也能懂：如何在“已经运行中的服务器 Docker”上更新 Java 后端代码（无 CI/CD）

> 适用场景：你的服务器上已经有一套用 `docker compose` 跑起来的服务（后端、MySQL、Redis、MinIO、前端等），你本地把后端代码改好了，现在想把代码更新到服务器里运行的后端容器中。
>
> 这份文档会先讲清楚**大纲**与**核心概念（原理 + 类比）**，再给出**可复制粘贴的步骤**。  
> 你同事说“很简单”，通常是因为他们脑子里已经非常清楚：**Docker 镜像 vs 容器 vs 挂载（volume/bind mount）**这三者的关系。

---

## 大纲（先给你一张路线图）

- **1. 你现在到底是哪一种运行方式？（最关键）**
  - **A. 开发模式：源码挂载（bind mount）到容器里 + 容器里跑 `mvn spring-boot:run`**
  - **B. 镜像模式：代码被打包进镜像（JAR 在镜像里）+ 容器只是运行这个镜像**
- **2. 两种方式各自怎么更新代码？**
  - A：更新服务器上的源码 → 容器自动/手动重启应用（不一定要重建镜像）
  - B：更新源码 → 重建镜像 → 用新镜像重建容器（重启容器本身不够）
- **3. 安全与数据：为什么你“重建后端容器”通常不会丢 MySQL/Redis/MinIO 数据？**
- **4. 推荐做法：开发环境与生产环境分别应该怎么跑（避免线上跑 devtools）**
- **5. 常见坑位排查：更新了但没生效/端口冲突/compose 找不到/权限问题**

---

## 1) 核心概念（技术原理 + 类比）

### 1.1 镜像（image） vs 容器（container）

- **镜像（image）**：像“软件安装包 / 冻结好的系统快照”。里面可能包含你的 `app.jar`、JDK、依赖等。
- **容器（container）**：像“正在运行的进程（带隔离的运行环境）”。它是从某个镜像“启动出来”的一次运行实例。

**类比**：
- 镜像 = “做好的蛋糕配方 + 烤好的蛋糕胚”
- 容器 = “把蛋糕拿出来摆到桌上开始吃的那个过程”

所以：
- 你**改代码**，如果代码是“烤在蛋糕里”（打进镜像），那你得**重新烤**（重建镜像）；
- 如果代码是“桌上现放的水果装饰”（挂载进容器的文件），你换水果（改宿主机文件）就会变。

### 1.2 Volume / Bind Mount（挂载）

Docker 有一种机制：把**宿主机上的文件夹**“映射”到容器里的某个路径。

- **bind mount**：直接把宿主机目录（比如 `./src`）挂到容器（比如 `/app/src`）
  - 你的仓库 `compose.dev.yml` 就是这种：`./src:/app/src`、`./pom.xml:/app/pom.xml`
- **named volume**：Docker 管理的一块“持久化磁盘空间”（通常用于数据库数据）

**类比**：
- bind mount = “把你电脑的一个文件夹，‘共享’给容器用；容器看到的其实是你电脑上的那份”
- named volume = “Docker 给你分了一块硬盘抽屉，容器删了重建，抽屉里的东西还在”

### 1.3 “重启容器”为什么不等于“更新代码”

很多新手最容易踩坑：
- `docker restart xxx` 只会**重启同一个容器**（仍然是老镜像 + 老文件系统）
- 如果你的代码在镜像里：不重建镜像，就不可能变新

**一句话**：
> “重启”只能让程序重新跑；“更新”要让程序拿到新代码（来自挂载或新镜像）。

---

## 2) 你们这个仓库的现实情况（我根据仓库文件帮你对上）

你们仓库里已经准备了两套 compose：

- **开发**：`compose.dev.yml` + `Dockerfile.dev`
  - 后端容器里跑：`mvn spring-boot:run ...`
  - 并且把宿主机的 `./src`、`./pom.xml` 挂进容器（热更新基础）
  - 使用 `.env.dev`，建议用脚本：`./docker-compose-dev.sh ...`
- **生产**：`compose.prod.yml` + `Dockerfile`
  - `Dockerfile` 会 `mvn package` 打出 JAR → JAR 被拷进运行时镜像
  - 后端通常不挂载 `src`（避免线上“改文件就变”的不可控）

你贴的服务器 `docker ps` 里：
- 有 `mysql`、`redis`、`minio`，而且都把端口映射到了宿主机
- 有 `backend`，也把 `8101` 映射到宿主机
- 有 `frontend`，把 `8080` 映射到宿主机

这更像是“开发态/测试态”的组合（因为端口都暴露出来了）。但**到底是不是源码挂载**，我们必须在服务器上确认一次。

---

## 3) 第一步：在服务器上确认“到底是哪种更新方式”（必做）

你只需要在服务器上跑两条命令，找到答案：

### 3.1 看容器是不是由 compose 管的（推荐）

```bash
docker inspect labos_backend-backend-1 --format '{{json .Config.Labels}}' | head -c 2000
```

你想看到类似这些字段（说明 compose 管理）：
- `com.docker.compose.project`
- `com.docker.compose.project.working_dir`
- `com.docker.compose.service`

**意义**：
- 你会知道“当初启动它的仓库目录在哪”
- 你会知道“用的 compose 项目名是什么”（例如 `labos_backend`）

### 3.2 看后端容器有没有挂载源码（决定更新方式 A 还是 B）

```bash
docker inspect labos_backend-backend-1 --format '{{json .Mounts}}' | head -c 4000
```

观察点：
- 如果你看到宿主机路径类似 `.../springboot-init-master/src` 映射到容器 `/app/src`  
  那就是 **方式 A（源码挂载）**
- 如果你基本看不到 `src`/`pom.xml` 这类挂载，只看到 `application.yml` 等少量文件  
  那通常就是 **方式 B（镜像内代码）**

**类比**：
- 看挂载就像看“容器吃的菜到底是厨房现做（挂载）还是罐头（镜像里）”

---

## 4) 更新方式 A：源码挂载（推荐开发/测试用）

### 4.1 这个模式的工作方式（你理解了就不怕了）

你们的 `compose.dev.yml` 是这么设计的：
- `./src:/app/src`：容器里的 `/app/src` 其实就是宿主机的 `./src`
- 容器启动命令是 `mvn spring-boot:run`（在 `Dockerfile.dev` 里）

因此更新代码的本质是：
> 你只要把服务器上的 `./src` 更新成新版本，容器里看到的就是新代码。

接下来 Spring DevTools 可能会自动触发重启；如果没有自动重启，你就手动重启后端服务容器即可。

### 4.2 你要先把“新代码”放到服务器（两种常见方式）

#### 方案 A1：服务器直接 `git pull`（最省事）

前提：服务器上有这个仓库目录，并且是用 git 管理的。

```bash
cd /path/to/springboot-init-master
git status
git pull
```

**意义**：更新“宿主机上的源码目录”。  
**为什么必须做这步**：源码挂载是“挂载宿主机的文件夹”，你本地改完不等于服务器也改完。

#### 方案 A2：你本地把代码传到服务器（scp/rsync）

如果服务器没 git 或不方便 pull，可以用 `rsync`（更适合大量小文件）：

```bash
rsync -av --delete ./src/ ubuntu@你的服务器:/path/to/springboot-init-master/src/
rsync -av ./pom.xml ubuntu@你的服务器:/path/to/springboot-init-master/pom.xml
```

**意义**：把你本地的“新 src/pom”同步到服务器挂载目录。

### 4.3 让后端应用吃到新代码（推荐操作顺序）

#### 最理想：什么都不用做，DevTools 自动重启

如果容器里真的在跑 `mvn spring-boot:run`，并且 DevTools 生效，文件变化会触发重启。

你可以观察日志确认：

```bash
docker logs -f labos_backend-backend-1
```

看到类似 “Restarting…” 或重新编译的日志，就说明自动生效了。

#### 如果没有自动重启：重启后端容器

```bash
docker restart labos_backend-backend-1
```

**注意**：这里“重启”之所以有效，是因为代码来自“挂载”，重启后会重新编译/重新运行时读取新代码。

#### 如果你改了依赖（pom.xml）或编译异常：重建后端容器更稳

建议用 compose 的方式，而不是直接 `docker run`：

```bash
cd /path/to/springboot-init-master
docker compose --env-file .env.dev -f compose.dev.yml up -d --build backend
```

或用你们仓库自带脚本（更不容易写错）：

```bash
cd /path/to/springboot-init-master
./docker-compose-dev.sh up -d --build backend
```

**意义**：
- `--build`：确保 Dockerfile.dev 相关层更新（例如你改了 Dockerfile.dev 或需要重新拉依赖层）
- `up -d backend`：只重建/重启后端，不动 MySQL/Redis/MinIO

---

## 5) 更新方式 B：代码在镜像里（生产常用）

### 5.1 这个模式的工作方式（关键理解）

`Dockerfile` 的设计是：
- 在 builder 阶段把代码复制进镜像，`mvn package` 生成 JAR
- 运行阶段只带一个 `app.jar`

因此更新代码的本质是：
> 你必须用新代码重新 build 一个新镜像，再用新镜像创建（recreate）容器。

只 `docker restart` 是不够的（因为重启还是老镜像）。

### 5.2 更新步骤（推荐最短正确路径）

#### Step 0：先更新服务器上的代码（同 4.2：git pull 或 rsync）

```bash
cd /path/to/springboot-init-master
git pull
```

#### Step 1：重建后端镜像

如果你们是用 `compose.prod.yml`：

```bash
cd /path/to/springboot-init-master
docker compose --env-file .env -f compose.prod.yml build backend
```

如果你们其实还是用 dev compose 但后端走 `Dockerfile`，也可以把 `-f` 换成实际文件。

#### Step 2：用新镜像重建并启动后端容器（不动其他服务）

```bash
docker compose --env-file .env -f compose.prod.yml up -d --no-deps --force-recreate backend
```

参数解释：
- `--no-deps`：不要连带重启 redis/nginx/frontend（降低影响面）
- `--force-recreate`：即使配置没变也强制重建容器（保证切到新镜像）

#### Step 3：验证是否生效

```bash
docker ps | grep backend
docker logs --tail 200 labos_backend-backend-1
```

你也可以用你们的健康检查路径（仓库里后端 healthcheck 是访问 `/api/doc.html`）：

```bash
curl -f http://127.0.0.1:8101/api/doc.html
```

---

## 6) “会不会把数据弄没？”——安全说明（非常重要）

### 6.1 一般不会丢数据的原因

数据库/缓存/对象存储的数据通常不在容器本身的“临时文件系统”里，而是在：
- bind mount 到宿主机目录（例如 `./data-dev/...`、`./data-prod/...`）
- 或者 named volume

所以你重建的是 `backend` 容器/镜像，**MySQL/Redis/MinIO 的数据目录不动**，数据就不会丢。

### 6.2 你最需要避免的危险操作

- **不要随便 `docker compose down -v`**
  - `-v` 可能会删除 named volume（等于把“抽屉”扔了）
- 不要随便 `rm -rf data-dev` / `data-prod`
  - 那是 bind mount 的“真实数据”

---

## 7) 最推荐的“最省事更新套路”（给你一个固定动作）

### 7.1 你确定是方式 A（源码挂载）时

```bash
cd /path/to/springboot-init-master
git pull
./docker-compose-dev.sh up -d backend
docker logs --tail 80 labos_backend-backend-1
```

> 如果你改了依赖/编译经常出问题，就把 `up -d backend` 改成 `up -d --build backend`。

### 7.2 你确定是方式 B（镜像内代码）时

```bash
cd /path/to/springboot-init-master
git pull
docker compose --env-file .env -f compose.prod.yml build backend
docker compose --env-file .env -f compose.prod.yml up -d --no-deps --force-recreate backend
docker logs --tail 120 labos_backend-backend-1
```

---

## 8) 常见问题排查（更新了但没生效？）

### 8.1 “我更新了代码，但接口还是旧的”

最常见原因：
- 你更新的是本地代码，但服务器目录没更新（没有 `git pull/rsync`）
- 容器根本没挂载 `src`，但你误以为是方式 A（其实是方式 B）
- 你只是 `docker restart` 了容器，但你需要 `build + recreate`

排查顺序（按性价比）：
- `docker inspect ...Mounts...` 看有没有挂载 `src`
- `docker logs -f backend` 看应用是否重新启动/重新编译
- `docker images | grep labos_backend-backend` 看镜像是否更新时间变化

### 8.2 “docker compose 命令找不到项目 / 找不到 yml”

你需要进入当初 compose 文件所在目录再运行：

```bash
pwd
ls
```

如果不知道目录，用这条从容器标签里找：

```bash
docker inspect labos_backend-backend-1 --format '{{index .Config.Labels "com.docker.compose.project.working_dir"}}'
```

### 8.3 “前端 unhealthy 会影响后端吗？”

一般不会影响后端更新，但如果你们用了 `depends_on: condition: service_healthy`，有些服务会等别的服务健康才启动。
你贴的 `docker ps` 里 `frontend` unhealthy，建议后面单独排（通常是健康检查 URL/端口或前端进程没起来）。

---

## 9) 建议（把“手动更新”变得更安全）

如果你们长期没有 CI/CD，建议至少做到：
- **把后端更新封装成一个固定脚本**（比如 `./deploy-backend.sh`），里面只做：
  - `git pull`
  - `docker compose build backend`
  - `docker compose up -d --no-deps --force-recreate backend`
- **给镜像打 tag**（例如把 git commit hash 写进 tag），便于回滚
- **保留上一版镜像**，出事可以快速回切

---

## 10) 你现在最该做什么（给你一个明确的下一步）

请你先在服务器上跑下面这条，把输出贴给我（重点是 Mounts 里有没有 `src -> /app/src`）：

```bash
docker inspect labos_backend-backend-1 --format '{{json .Mounts}}'
```

我就能**100% 确认你该走方式 A 还是 B**，然后我可以把“你们服务器当前实际启动方式”的那套命令写成一段最短 SOP（复制粘贴就能升级）。



