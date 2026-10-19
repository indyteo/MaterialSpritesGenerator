#!/usr/bin/env python3
"""Generate atlas sprite lists from a vanilla Minecraft client jar.

Resolves the atlas definitions (assets/minecraft/atlases/*.json) the same way the game does,
producing the same sprite names as an in-game F3 + S dump (without the texture coordinates).

Usage: extract_atlases.py <version | path/to/client.jar> [atlas ...]
"""
import io
import json
import os
import re
import sys
import urllib.request
import zipfile

MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
OUTPUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "atlases")
DEFAULT_ATLASES = ["blocks", "decorated_pot", "gui", "items", "map_decorations", "particles"]


def fetch_json(url):
	with urllib.request.urlopen(url) as response:
		return json.load(response)


def open_client_jar(arg):
	if os.path.isfile(arg):
		return zipfile.ZipFile(arg)
	versions = {v["id"]: v["url"] for v in fetch_json(MANIFEST)["versions"]}
	if arg not in versions:
		sys.exit(f"Unknown Minecraft version: {arg}")
	url = fetch_json(versions[arg])["downloads"]["client"]["url"]
	print(f"Downloading {url}")
	with urllib.request.urlopen(url) as response:
		return zipfile.ZipFile(io.BytesIO(response.read()))


def namespaced(resource):
	return resource if ":" in resource else "minecraft:" + resource


def resolve(jar, atlas):
	textures = "assets/minecraft/textures/"
	pngs = [n[len(textures):-len(".png")] for n in jar.namelist() if n.startswith(textures) and n.endswith(".png")]
	sprites = set()
	for source in json.loads(jar.read(f"assets/minecraft/atlases/{atlas}.json"))["sources"]:
		kind = source["type"].removeprefix("minecraft:")
		if kind == "directory":
			directory = source["source"] + "/"
			sprites.update("minecraft:" + source["prefix"] + p[len(directory):] for p in pngs if p.startswith(directory))
		elif kind == "single":
			sprites.add(namespaced(source.get("sprite", source["resource"])))
		elif kind == "paletted_permutations":
			separator = source.get("separator", "_")
			for texture in source["textures"]:
				sprites.update(namespaced(texture) + separator + key for key in source["permutations"])
		elif kind == "filter":
			pattern = source["pattern"]
			namespace = re.compile(pattern.get("namespace", ".*"))
			path = re.compile(pattern.get("path", ".*"))
			sprites = {s for s in sprites if not (namespace.search(s.split(":", 1)[0]) and path.search(s.split(":", 1)[1]))}
		else:
			sys.exit(f"Unsupported atlas source type '{kind}' in {atlas}")
	sprites.add("minecraft:missingno")
	return sorted(sprites)


def main():
	if len(sys.argv) < 2:
		sys.exit(__doc__)
	jar = open_client_jar(sys.argv[1])
	for atlas in sys.argv[2:] or DEFAULT_ATLASES:
		sprites = resolve(jar, atlas)
		with open(os.path.join(OUTPUT, atlas + ".txt"), "w", newline="\n") as file:
			file.writelines(s + "\n" for s in sprites)
		print(f"{atlas}: {len(sprites)} sprites")


if __name__ == "__main__":
	main()
