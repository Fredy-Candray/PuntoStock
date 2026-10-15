# Git y GitHub

Se ha elegido monorepo: cambios de API, DDL y cliente se revisan en un mismo commit. Git local usa la rama `main`. Destino indicado: [Fredy-Candray/PuntoStock](https://github.com/Fredy-Candray/PuntoStock). El remoto `origin` apunta a `https://github.com/Fredy-Candray/PuntoStock.git`.

Desde la raíz, si todavía no está inicializado:

```powershell
git init -b main
git config user.name "Tu nombre"
git config user.email "tu-correo"
git status --short
git add .
git commit -m "feat: base de ventas e inventario"
```

Con GitHub CLI instalado y autenticado, crear un repositorio privado en la cuenta elegida:

```powershell
gh auth login
gh repo create OWNER/PuntoStock --private --source=. --remote=origin --push
```

Si el repositorio vacío ya existe:

```powershell
git remote add origin https://github.com/OWNER/PuntoStock.git
git push -u origin main
git remote -v
```

En este proyecto `origin` ya está configurado: después de revisar y crear el commit, usar `git push -u origin main`. Si el remoto contiene historial, obtenerlo e integrarlo primero; no usar `--force`.

Reemplazar `OWNER` por el propietario autorizado. No ejecutar ambos métodos. Si `origin` ya existe, inspeccionarlo antes de cambiar su URL. Nunca incluir `.env`, claves JWT, contraseñas ni tokens. Autenticar con Git Credential Manager o SSH, sin colocar tokens en URLs.

Trabajo habitual:

```powershell
git switch -c feat/nueva-funcionalidad
git add .
git commit -m "feat: descripción"
git push -u origin feat/nueva-funcionalidad
gh pr create --base main
```

Configurar protección de `main`, revisión por PR y checks de Maven y Angular en GitHub. Para repos separados, conservar el mismo contrato API y versionar cambios compatibles; no es necesario para esta fase.
