Fast Thickmap ImageJ/Fiji plugin
================================

This repository contains the source code of the Fast Thickmap ImageJ/Fiji plugin.
The algorithm used in the plugin is described in [1] and it improves on the performance of the algoritm described in [2].


Getting it
----------

You can download the plugin from the [GitHub Releases page](https://github.com/arttumiettinen/fast-thickmap-imagej/releases), or by enabling update site
https://sites.imagej.net/AMiettinen/ in the Fiji Updater.


Documentation
-------------

Documentation of the plugin can be found at [ReadTheDocs](https://fast-thickmap-imagej.readthedocs.io/en/latest/).


Building
--------

Eclipse project files and a .jardesc file are provided along with the repository. Please use "File->Import->General->Existing Projects into Workspace" wizard to import the project to your Eclipse workspace.

Remember to compile for Java runtime that is compatible with ImageJ/Fiji!



References
----------

[1] A. Miettinen, G. Lovric, E. Borisova, M. Stampanoni, Separable distributed local thickness algorithm for efficient morphological characterization of terabyte-scale volume images, in press, 2020.

[2] T. Hildebrand and P. Ruegsegger, A new method for the model-independent assessment of thickness in three-dimensional images, Journal of Microscopy 185(1), 67–75, 1997.



