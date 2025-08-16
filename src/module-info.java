module com.github.david32768.JynxFor {
	exports com.github.david32768.jynxfor.ops;
	requires org.objectweb.asm;
	requires org.objectweb.asm.tree;
	requires org.objectweb.asm.tree.analysis;
	requires org.objectweb.asm.util;
        requires com.github.david32768.JynxFree;
 	uses com.github.david32768.jynxfor.ops.MacroLib;
        provides com.github.david32768.jynxfree.jynx.MainOptionService
                with com.github.david32768.jynxfor.MainJynx,
                com.github.david32768.jynxfor.MainVerify;
}
