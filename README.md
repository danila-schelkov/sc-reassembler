# SC Reassembler

[RU](./docs/README.RU.md)

SC Reassembler is a demonstration project showing how the modules of
[SC Editor](https://github.com/danila-schelkov/sc-editor) can be used for custom purposes.
The main purpose of the project is to downgrade SC2 files and save them in the older SC format.

## Features

* Managing exports: allows excluding exports from a file by name or,
  conversely, assembling a file only from the selected exports.

* Downgrading file versions: converting SC2 files to SC format,
  which has a wider range of editing tools available.

## How to use?

1. Clone the repository:

   ```bash
   git clone https://github.com/danila-schelkov/sc-reassembler.git
   ```

2. Build the project to all-in-one jar file:

   ```bash
   cd sc-reassembler
   ./gradlew shadowJar
   ```

3. Run the application:

   ```bash
   ./gradlew run --args="[options]"  # or
   java -jar build/libs/sc-reassembler-{version}-all.jar [options]
   ```

Available options:

- `--exports <text file name>`: specify a file with the required export names.

- `--negate`: if this flag is set, the exports listed in the exports file will be excluded
  from the output file instead of being added to it.

- `--files <path>`: specify the path to the input SC or SC2 file.

- `--directory <path>`: specify the path to a folder with the required SC files.

Example usage:

```bash
java -jar build/libs/sc-reassembler.jar --files path/to/input.sc --exports exports.txt
```

After running this command, a folder named `reassembled` will appear next to `path/to/input.sc`,
containing the new SC file. It will include only the exports listed in `exports.txt`.

## Dependencies

* Supercell SWF: used to work with SC and SC2 format files.

* FlatBuffers: required to generate classes from `.fbs` files, since
  some classes of the Supercell SWF library are overridden (see
  [Technical Details](#technical-details)).

## Technical Details

Certain classes from the Supercell SWF library are overridden in this project.
Therefore, FlatBuffers class generation from `.fbs` files must be included in the project.
This process is set up in Gradle using the corresponding plugins.

If you do not intend to override library classes that use FlatBuffers,
you can omit FlatBuffers-related configuration in your Gradle project.

I usually avoid including generated files in the git repository,
but here they are included for demonstration purposes.

## License

Starting from commit adba913 (exclusive), this project is distributed under the MIT License.
See the [LICENSE](LICENSE) file for details.

All code up to and including commit adba913 was distributed under the GNU GPLv3.

---

You might have been looking for SC Editor or information about it;
if so, [follow this link](https://github.com/danila-schelkov/sc-editor).

If you have any questions or suggestions, please create an Issue.
