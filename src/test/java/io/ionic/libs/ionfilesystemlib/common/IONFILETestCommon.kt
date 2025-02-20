package io.ionic.libs.ionfilesystemlib.common

import android.net.Uri

/**
 * Uri.parse expects a the uri string to already be encoded, so this is a small helper method
 *  to make sure that spaces and other characters get encoded
 */
internal fun fileUriWithEncodings(uriString: String): Uri =
    Uri.parse(Uri.encode(uriString, "/:"))

internal const val LOREM_IPSUM_2800_CHARS = """
Lorem ipsum dolor sit amet, consectetur adipiscing elit.
Sed quis mauris nec lorem sollicitudin tristique sagittis tempor odio.
Sed feugiat sem sit amet ligula dapibus, a mattis metus tincidunt. Nam at est libero.
Morbi maximus justo felis, sit amet varius mi luctus ac.
Nunc aliquet, tortor nec dignissim porta, quam ipsum dapibus tortor, vel pharetra odio turpis eget urna.
Donec bibendum elementum lorem, ac tempus tortor pharetra ac. Fusce aliquam consequat fermentum.

Donec magna ligula, congue a nisl ac, facilisis rhoncus magna.
Suspendisse potenti. Nullam sed odio mollis, condimentum eros quis, laoreet ex.
Pellentesque velit sapien, hendrerit quis consectetur in, semper vel enim.
Duis varius eleifend elementum.
Pellentesque convallis eleifend urna non scelerisque.
In malesuada dignissim metus commodo mattis. Suspendisse a nulla ut risus elementum tristique.
Suspendisse laoreet mauris est, nec bibendum urna rhoncus id.
Praesent congue ipsum ultrices ultricies commodo.
Vestibulum eget dolor diam. Duis egestas justo ac risus efficitur feugiat.

Nunc erat lorem, lacinia in dui quis, ultricies vestibulum lectus.
Vestibulum et tortor id ante lacinia dignissim.
Quisque lacinia ullamcorper lectus at malesuada.
Proin et turpis eget mi fermentum euismod.
Morbi ante massa, hendrerit sed quam at, sagittis ultrices leo.
Vivamus finibus facilisis enim. Ut tristique lorem id eleifend commodo.
Etiam tincidunt ligula non elit congue consectetur.
Morbi posuere elit libero, id eleifend arcu aliquet sed.

Aliquam euismod arcu suscipit, commodo orci vel, blandit metus.
Donec condimentum convallis risus, quis viverra felis consequat non.
Cras tristique, dolor in tincidunt vulputate, turpis mauris faucibus quam, et vestibulum ante felis in urna.
Integer et magna dictum, placerat felis et, tempus lectus. Donec vel posuere mauris.
Vivamus commodo ipsum diam, in laoreet risus vulputate ut. Quisque cursus magna neque.
Mauris dapibus rhoncus libero eu sollicitudin.
Donec lobortis, augue ut vestibulum varius, nulla nibh hendrerit nisl, id rutrum magna ipsum ut purus.
Sed a consectetur arcu, at scelerisque lorem. Maecenas a erat est.
Nulla et pulvinar nisl, eget ultricies ipsum. In fermentum malesuada augue.
Proin auctor vulputate justo id ultrices. Nunc dictum placerat diam, ut suscipit urna lacinia a.

Praesent laoreet eget sem nec fringilla.
Proin est metus, tempor sit amet congue a, vulputate a purus.
Maecenas ut tristique mi. Fusce sed justo turpis. Aenean vitae dui ipsum.
Nam in purus sit amet enim sagittis fringilla.
Curabitur libero orci, condimentum quis volutpat et, placerat id massa.
Curabitur ex sem, congue id congue ac, tempor eu augue.
Proin mi tortor, malesuada eget nunc convallis, sollicitudin ultrices nisi.
Nullam venenatis nisl et neque mollis maximus.

(...)
"""