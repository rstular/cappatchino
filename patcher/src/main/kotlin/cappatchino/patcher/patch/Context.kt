package cappatchino.patcher.patch

import cappatchino.patcher.utils.proxy.ProxyClassInfoList

sealed interface Context

class BytecodeContext internal constructor(val classes: ProxyClassInfoList) : Context
