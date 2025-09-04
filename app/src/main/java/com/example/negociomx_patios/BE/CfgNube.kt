package com.example.negociomx_patios.BE

data class CfgNube(
    var IdCfg:String?=null,
    var IdCfgLocal:String?=null,
    var IdEmpresa:String?=null,
    var Predeterminada:Boolean=false,
    var ConsecutivoFolioPago:Int=0,
    var PrefijoFolioPago:String="",
    var NombreTipoPagoPredeterminado:String?=null,
    var Activa:Boolean?=null,
)
