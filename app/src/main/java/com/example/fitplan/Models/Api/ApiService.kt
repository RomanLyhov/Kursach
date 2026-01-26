    package com.example.fitplan.Models.Api

    import retrofit2.Call
    import retrofit2.http.GET
    import retrofit2.http.Query

    interface ApiService {

        @GET("cgi/search.pl")
        fun searchProducts(
            @Query("search_terms") query: String,
            @Query("search_simple") simple: Int = 1,
            @Query("json") json: Int = 1,
            @Query("page_size") pageSize: Int = 10
        ): Call<ProductResponse>
    }