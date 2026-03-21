# Local website folder (not in this repo)

The `website/` directory is **gitignored** so your MineTeh Android project does not track or push the website codebase.

## One-time clone

From the **MineTehApp** project root (this folder), run (replace with your real URL):

```bash
git clone <YOUR_WEBSITE_REPO_URL> website
```

Examples:

```bash
git clone https://github.com/your-org/mineteh-website.git website
git clone git@github.com:your-org/mineteh-website.git website
```

## If `website` is not empty

Either remove/rename the folder first, or clone into a subfolder and move files.

## If Git still tracks old `website` files

If you ever committed `website/` before adding the ignore rule:

```bash
git rm -r --cached website
git commit -m "Stop tracking local website clone"
```
