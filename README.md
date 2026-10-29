# Navneet's Command Center

A responsive, dark-themed personal dashboard for published websites, GitHub projects, home-lab services, developer tools, career resources, and publishing links.

## Open locally
Open `index.html` in any modern browser. No build step, server, framework, API key, or dependency is required.

## Included
- Searchable shortcut cards and category filters
- Published sites: portfolio, Webline Cloud, IsItFree?, Piano, home-loan prepayment, and RO Water Solution
- GitHub projects and developer resources
- Home-lab shortcuts for FileBrowser Quantum, HomeLab One, Grafana, Prometheus, and Zabbix
- Add-your-own shortcut form
- Personal notes auto-saved in browser local storage
- Local date/time and responsive layout

## Personalize your setup
Some home-lab URLs are placeholders (for example `http://lab-ubuntu:8081`). Edit the `initialLinks` array in `index.html` to replace them with your real hostnames, IP addresses, ports, and Cloudflare Tunnel URLs. Add any missing tools using **Add link**; custom links are stored only in the current browser.

## Publish with GitHub Pages
In the repository settings, open **Pages**, choose **Deploy from a branch**, select `main` and `/ (root)`, then save. After deployment, GitHub will show the public Pages URL in that settings section.

## Privacy note
This is a static page. Notes and shortcuts added through the UI stay in the current browser's local storage and do not sync to other devices. Do not store passwords, tokens, or other secrets in notes or URLs.
